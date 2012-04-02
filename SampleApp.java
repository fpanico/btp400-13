


package Provider.GoogleMapsStatic.TestUI;

import javax.swing.event.*;
import Provider.GoogleMapsStatic.*;
import Task.*;
import Task.Manager.*;
import Task.ProgressMonitor.*;
import Task.Support.CoreSupport.*;
import Task.Support.GUISupport.*;
import com.intellij.uiDesigner.core.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import info.clearthought.layout.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.beans.*;
import java.text.*;
import java.util.concurrent.*;
import org.jdesktop.beansbinding.*;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.swingx.*;

/** 
 * This application allows users to generate queries in order to retrieve maps from google static maps service
 * using latitudes and longitudes. It also allows users to pan by 25km in any direction and also to zoom in and out. 
 * This app used JFORMDESIGNER to help generate the swing layout. We maintained the declaration and instantiation layout from
 * the JFORMDESIGNER and added our own components there so as to not redo the entire layout. 
 * @author Tudor Minea, Frank Panico - building on code by nazmul idris */
public class SampleApp extends JFrame {

/** reference to task */
private SimpleTask _task;
/** holds image from query to google maps  */
private BufferedImage _img;
/** this might be null. holds the text in case image doesn't display */
private String _respStr;
/**Array of Strings holding   */
public String[] cities = {"Choose a starting point", "Toronto", "Montreal",
			 "Calgary", "Quebec", "Ottawa", "Winnipeg", "Edmonton", "Victoria",
			 "Fredericton", "Halifax", "Regina"};

//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
// main method...
//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

public static void main(String[] args) {
  Utils.createInEDT(SampleApp.class);
}

//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
// constructor
//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

private void doInit() {
  GUIUtils.setAppIcon(this, "burn.png");
  GUIUtils.centerOnScreen(this);
  setVisible(true);

  int W = 35, H = W;
  boolean blur = false;
  float alpha = 1f;
  

  
  
  

  try {
  }
  catch (Exception e) {
    System.out.println(e);
  }

  _setupTask();
}

/** create a test task and wire it up with a task handler that dumps output to the textarea */
@SuppressWarnings("unchecked")
private void _setupTask() {

  TaskExecutorIF<ByteBuffer> functor = new TaskExecutorAdapter<ByteBuffer>() {
    public ByteBuffer doInBackground(Future<ByteBuffer> swingWorker,
                                     SwingUIHookAdapter hook) throws Exception
    {

      _initHook(hook);

      // get the uri for the static map
      String uri = MapLookup.getMap(Double.parseDouble(ttfLat.getText()),
                                    Double.parseDouble(ttfLon.getText()),
                                    512,
                                    512,
                                    Integer.parseInt(ttfZoom.getText())
      );
      sout("Google Maps URI=" + uri);

      // get the map from Google
      GetMethod get = new GetMethod(uri);
      new HttpClient().executeMethod(get);

      ByteBuffer data = HttpUtils.getMonitoredResponse(hook, get);

      try {
        _img = ImageUtils.toCompatibleImage(ImageIO.read(data.getInputStream()));
        sout("converted downloaded data to image...");
      }
      catch (Exception e) {
        _img = null;
        sout("The URI is not an image. Data is downloaded, can't display it as an image.");
        _respStr = new String(data.getBytes());
      }

      return data;
    }

    @Override public String getName() {
      return _task.getName();
    }
  };

  _task = new SimpleTask(
      new TaskManager(),
      functor,
      "HTTP GET Task",
      "Download an image from a URL",
      AutoShutdownSignals.Daemon
  );

  _task.addStatusListener(new PropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent evt) {
      sout(":: task status change - " + ProgressMonitorUtils.parseStatusMessageFrom(evt));
      lblProgressStatus.setText(ProgressMonitorUtils.parseStatusMessageFrom(evt));
    }
  });

  _task.setTaskHandler(new
      SimpleTaskHandler<ByteBuffer>() {
        @Override public void beforeStart(AbstractTask task) {
          sout(":: taskHandler - beforeStart");
        }
        @Override public void started(AbstractTask task) {
          sout(":: taskHandler - started ");
        }
        /** {@link SampleApp#_initHook} adds the task status listener, which is removed here */
        @Override public void stopped(long time, AbstractTask task) {
          sout(":: taskHandler [" + task.getName() + "]- stopped");
          sout(":: time = " + time / 1000f + "sec");
          task.getUIHook().clearAllStatusListeners();
        }
        @Override public void interrupted(Throwable e, AbstractTask task) {
          sout(":: taskHandler [" + task.getName() + "]- interrupted - " + e.toString());
        }
        @Override public void ok(ByteBuffer value, long time, AbstractTask task) {
          sout(":: taskHandler [" + task.getName() + "]- ok - size=" + (value == null
              ? "null"
              : value.toString()));
          if (_img != null) {
            _displayImgInFrame();
          }
          else _displayRespStrInFrame();

        }
        @Override public void error(Throwable e, long time, AbstractTask task) {
          sout(":: taskHandler [" + task.getName() + "]- error - " + e.toString());
        }
        @Override public void cancelled(long time, AbstractTask task) {
          sout(" :: taskHandler [" + task.getName() + "]- cancelled");
        }
      }
  );
}

private SwingUIHookAdapter _initHook(SwingUIHookAdapter hook) {
  hook.enableRecieveStatusNotification(checkboxRecvStatus.isSelected());
  hook.enableSendStatusNotification(checkboxSendStatus.isSelected());

  hook.setProgressMessage(ttfProgressMsg.getText());

  PropertyChangeListener listener = new PropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent evt) {
      SwingUIHookAdapter.PropertyList type = ProgressMonitorUtils.parseTypeFrom(evt);
      int progress = ProgressMonitorUtils.parsePercentFrom(evt);
      String msg = ProgressMonitorUtils.parseMessageFrom(evt);

      progressBar.setValue(progress);
      progressBar.setString(type.toString());

      sout(msg);
    }
  };

  hook.addRecieveStatusListener(listener);
  hook.addSendStatusListener(listener);
  hook.addUnderlyingIOStreamInterruptedOrClosed(new PropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent evt) {
      sout(evt.getPropertyName() + " fired!!!");
    }
  });

  return hook;
}
/**Displays the static map image to the left of the application window */
private void _displayImgInFrame() {



 PictureBox.setIcon(new ImageIcon(_img));
  PictureBox.setToolTipText(MessageFormat.format("<html>Image downloaded from URI<br>size: w={0}, h={1}</html>",
                                             _img.getWidth(), _img.getHeight()));

}

private void _displayRespStrInFrame() {

  final JFrame frame = new JFrame("Google Static Map - Error");
  GUIUtils.setAppIcon(frame, "69.png");
  frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

  JTextArea response = new JTextArea(_respStr, 25, 80);
  response.addMouseListener(new MouseListener() {
    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) { frame.dispose();}
    public void mouseReleased(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
  });

  frame.setContentPane(new JScrollPane(response));
  frame.pack();

  GUIUtils.centerOnScreen(frame);
  frame.setVisible(true);
}

/** simply dump status info to the textarea */
private void sout(final String s) {
  Runnable soutRunner = new Runnable() {
    public void run() {
      if (ttaStatus.getText().equals("")) {
        ttaStatus.setText(s);
      }
      else {
        ttaStatus.setText(ttaStatus.getText() + "\n" + s);
      }
    }
  };

  if (ThreadUtils.isInEDT()) {
    soutRunner.run();
  }
  else {
    SwingUtilities.invokeLater(soutRunner);
  }
}

private void startTaskAction() {
  try {
    _task.execute();
  }
  catch (TaskException e) {
    sout(e.getMessage());
  }
}


public SampleApp() {
  initComponents();
  doInit();
}
/** terminates the program */
private void quitProgram() {
  _task.shutdown();
  System.exit(0);
}
/**
 * adds 0.25 to the current latitude value
 *  */
private void LatPlus() {
		
	String x = ttfLat.getText();
	Double y = Double.parseDouble(x);
	y +=0.25;
	x = y.toString();
	ttfLat.setText(x);
	
	
}
/**
 * subtracts 0.25 from the current latitude value
 *  */
private void LatMinus(){
	String x = ttfLat.getText();
	Double y = Double.parseDouble(x);
	y -=0.25;
	x = y.toString();
	ttfLat.setText(x);
	
}
/**
 * adds 0.25 to the current longitude value
 *  */
private void longPlus(){
	String x = ttfLon.getText();
	Double y = Double.parseDouble(x);
	y +=0.25;
	x = y.toString();
	ttfLon.setText(x);
}
/**
 * subtracts 0.25 from the current longitude value
 *  */
private void longMinus(){
	String x = ttfLon.getText();
	Double y = Double.parseDouble(x);
	y -=0.25;
	x = y.toString();
	ttfLon.setText(x);
}
/**Regenerates the map, moving the center 25 km NE */
private void panNEActionPerformed(ActionEvent e) {
	  LatPlus();
	  longPlus();
	  startTaskAction();
}
/**Regenerates the map, moving the center 25 km N */
private void panNActionPerformed(ActionEvent e) {
	  LatPlus();
	
	startTaskAction();
}
/**Regenerates the map, moving the center 25 km NW */
private void panNWActionPerformed(ActionEvent e) {
	 longMinus();
	 LatPlus();

	  startTaskAction();
}


/**Regenerates the map, moving the center 25 km W */
private void panWActionPerformed(ActionEvent e) {
	  longMinus();  
	
	  startTaskAction();
}
/**Regenerates the map, moving the center 25 km E */
private void panEActionPerformed(ActionEvent e) {
	longPlus();
	  startTaskAction();
}
/**Regenerates the map, moving the center 25 km SE */
private void panSEActionPerformed(ActionEvent e) {
	LatMinus();
	  longPlus();
	 startTaskAction();
}
/**Regenerates the map, moving the center 25 km S */
private void panSActionPerformed(ActionEvent e) {
	LatMinus();
	  startTaskAction();
}

/**Regenerates the map, moving the center 25 km SW */
private void panSWActionPerformed(ActionEvent e) {
	  LatMinus();
	  longMinus();
	  startTaskAction();
}

/**
 * sets zoom value based on slider bar position
 *  */
private void slider1StateChanged(ChangeEvent e) {
	ttfZoom.setText(Integer.toString(slider1.getValue()));
	startTaskAction();
}

/**
 * Sets the latitude and longitude based on a choice from the combobox1. 
 * This combobox is populated from the cities string array
 */ 
private void comboBox1ActionPerformed(ActionEvent e) {
	switch (comboBox1.getSelectedIndex()){
		
	case 0:
		break;
	case 1:
		ttfLat.setText(Double.toString(43.666667));
		ttfLon.setText(Double.toString(-79.4));
		break;
	case 2:
		ttfLat.setText(Double.toString(45.5));
		ttfLon.setText(Double.toString(-73.583333));
		break;
	case 3:
		ttfLat.setText(Double.toString(51));
		ttfLon.setText(Double.toString(-114.166667));
		break;
	case 4:
		ttfLat.setText(Double.toString(46.866667));
		ttfLon.setText(Double.toString(-71.216667));
		break;
	case 5:
		ttfLat.setText(Double.toString(45.25));
		ttfLon.setText(Double.toString(-75.7));
		break;
	case 6:
		ttfLat.setText(Double.toString(50.63333));
		ttfLon.setText(Double.toString(-96.316667));
		break;
	case 7:
		ttfLat.setText(Double.toString(53.5));
		ttfLon.setText(Double.toString(-113.5));
		break;
	case 8:
		ttfLat.setText(Double.toString(48.5));
		ttfLon.setText(Double.toString(-123.416667));
		break;
	case 9: 
		ttfLat.setText(Double.toString(45.95));
		ttfLon.setText(Double.toString(-66.666667));
		break;		
	case 10:
		ttfLat.setText(Double.toString(44.633333));
		ttfLon.setText(Double.toString(-63.583333));	
		break;
	case 11:
		ttfLat.setText(Double.toString(50.45));
		ttfLon.setText(Double.toString(-105));	
		break;		
	default:
		break;
	
		
	}
}

private void goButtonActionPerformed(ActionEvent e) {
	startTaskAction();
}
















private void initComponents() {
  // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
  // Generated using JFormDesigner Evaluation license - Billy Thorton
  dialogPane = new JPanel();
  contentPanel = new JPanel();
  panel1 = new JPanel();
  label5 = new JLabel();
  ttfLon = new JTextField();
  panNE = new JButton();
  panN = new JButton();
  panNW = new JButton();
  label4 = new JLabel();
  ttfLat = new JTextField();
  panW = new JButton();
  button8 = new JButton();
  panE = new JButton();
  btnQuit = new JButton();
  label1 = new JLabel();
  ttfZoom = new JTextField();
  panSW = new JButton();
  panS = new JButton();
  panSE = new JButton();
  slider1 = new JSlider();
  comboBox1 = new JComboBox(cities);
  scrollPane1 = new JScrollPane();
  ttaStatus = new JTextArea();
  panel2 = new JPanel();
  panel3 = new JPanel();
  checkboxRecvStatus = new JCheckBox();
  checkboxSendStatus = new JCheckBox();
  ttfProgressMsg = new JTextField();
  progressBar = new JProgressBar();
  lblProgressStatus = new JLabel();
  PictureBox = new JLabel();

  //======== this ========
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
  setTitle("BTP400 Tudor and Frank \"Team 13\" Assignment 1");
  setIconImage(null);
  Container contentPane = getContentPane();
  contentPane.setLayout(new BorderLayout());

  //======== dialogPane ========
  {
	  dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
	  dialogPane.setOpaque(false);

	  // JFormDesigner evaluation mark
	  dialogPane.setBorder(new javax.swing.border.CompoundBorder(
		  new javax.swing.border.TitledBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0),
			  "JFormDesigner Evaluation", javax.swing.border.TitledBorder.CENTER,
			  javax.swing.border.TitledBorder.BOTTOM, new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
			  java.awt.Color.red), dialogPane.getBorder())); dialogPane.addPropertyChangeListener(new java.beans.PropertyChangeListener(){public void propertyChange(java.beans.PropertyChangeEvent e){if("border".equals(e.getPropertyName()))throw new RuntimeException();}});

	  dialogPane.setLayout(new BorderLayout());

	  //======== contentPanel ========
	  {
		  contentPanel.setOpaque(false);
		  contentPanel.setLayout(new TableLayout(new double[][] {
			  {TableLayout.FILL},
			  {TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED}}));
		  ((TableLayout)contentPanel.getLayout()).setHGap(5);
		  ((TableLayout)contentPanel.getLayout()).setVGap(5);

		  //======== panel1 ========
		  {
			  panel1.setOpaque(false);
			  panel1.setBorder(new CompoundBorder(
				  new TitledBorder("Configure the inputs to Google Static Maps"),
				  Borders.DLU2_BORDER));

			  //---- label5 ----
			  label5.setText("Longitude");
			  label5.setHorizontalAlignment(SwingConstants.RIGHT);

			  //---- ttfLon ----
			  ttfLon.setText("0");

			  //---- panNE ----
			  panNE.setText("NE");
			  panNE.addActionListener(new ActionListener() {
				  @Override
				  public void actionPerformed(ActionEvent e) {
					  panNEActionPerformed(e);
				  }
			  });

			  //---- panN ----
			  panN.setText("N");
			  panN.addActionListener(new ActionListener() {
				  @Override
				  public void actionPerformed(ActionEvent e) {
					  panNActionPerformed(e);
				  }
			  });

			  //---- panNW ----
			  panNW.setText("NW");
			  panNW.addActionListener(new ActionListener() {
				  @Override
				  public void actionPerformed(ActionEvent e) {
					  panNWActionPerformed(e);
				  }
			  });

			  //---- label4 ----
			  label4.setText("Latitude");
			  label4.setHorizontalAlignment(SwingConstants.RIGHT);

			  //---- ttfLat ----
			  ttfLat.setText("0");

			  //---- panW ----
			  panW.setText("W");
			  panW.addActionListener(new ActionListener() {
				  @Override
				  public void actionPerformed(ActionEvent e) {
					  panWActionPerformed(e);
				  }
			  });

			  //---- button8 ----
			  button8.setText("Go");
			  button8.addActionListener(new ActionListener() {
				  @Override
				  public void actionPerformed(ActionEvent e) {
					  goButtonActionPerformed(e);
				  }
			  });

			  //---- panE ----
			  panE.setText("E");
			  panE.addActionListener(new ActionListener() {
				  @Override
				  public void actionPerformed(ActionEvent e) {
					  panEActionPerformed(e);
				  }
			  });

			  //---- btnQuit ----
			  btnQuit.setText("Quit");
			  btnQuit.setMnemonic('Q');
			  btnQuit.setHorizontalAlignment(SwingConstants.LEFT);
			  btnQuit.setHorizontalTextPosition(SwingConstants.RIGHT);
			  btnQuit.addActionListener(new ActionListener() {
				  @Override
				  public void actionPerformed(ActionEvent e) {
					  quitProgram();
				  }
			  });

			  //---- label1 ----
			  label1.setText("Zoom");

			  //---- ttfZoom ----
			  ttfZoom.setText("7");
			  ttfZoom.setEditable(false);

			  //---- panSW ----
			  panSW.setText("SW");
			  panSW.addActionListener(new ActionListener() {
				  @Override
				  public void actionPerformed(ActionEvent e) {
					  panSWActionPerformed(e);
				  }
			  });

			  //---- panS ----
			  panS.setText("S");
			  panS.addActionListener(new ActionListener() {
				  @Override
				  public void actionPerformed(ActionEvent e) {
					  panSActionPerformed(e);
				  }
			  });

			  //---- panSE ----
			  panSE.setText("SE");
			  panSE.addActionListener(new ActionListener() {
				  @Override
				  public void actionPerformed(ActionEvent e) {
					  panSEActionPerformed(e);
				  }
			  });

			  //---- slider1 ----
			  slider1.setMaximum(15);
			  slider1.setValue(7);
			  slider1.setMajorTickSpacing(1);
			  slider1.setMinorTickSpacing(1);
			  slider1.addChangeListener(new ChangeListener() {
				  @Override
				  public void stateChanged(ChangeEvent e) {
					  slider1StateChanged(e);
				  }
			  });

			  //---- comboBox1 ----
			  comboBox1.setMaximumRowCount(12);
			  comboBox1.setToolTipText("Choose a Canadian City as a starting point or enter a custom latitude and longitude ");
			  comboBox1.addActionListener(new ActionListener() {
				  @Override
				  public void actionPerformed(ActionEvent e) {
					  comboBox1ActionPerformed(e);
				  }
			  });

			  GroupLayout panel1Layout = new GroupLayout(panel1);
			  panel1.setLayout(panel1Layout);
			  panel1Layout.setHorizontalGroup(
				  panel1Layout.createParallelGroup()
					  .addGroup(panel1Layout.createSequentialGroup()
						  .addGap(81, 81, 81)
						  .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
							  .addGroup(panel1Layout.createSequentialGroup()
								  .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
									  .addComponent(label5)
									  .addComponent(label4)
									  .addComponent(label1))
								  .addGap(18, 18, 18)
								  .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
									  .addComponent(ttfZoom, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									  .addComponent(ttfLat, GroupLayout.DEFAULT_SIZE, 83, Short.MAX_VALUE)
									  .addComponent(ttfLon)))
							  .addComponent(slider1, 0, 0, Short.MAX_VALUE))
						  .addGap(66, 66, 66)
						  .addGroup(panel1Layout.createParallelGroup()
							  .addGroup(panel1Layout.createSequentialGroup()
								  .addComponent(panNW)
								  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
								  .addComponent(panN)
								  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
								  .addComponent(panNE))
							  .addGroup(panel1Layout.createSequentialGroup()
								  .addComponent(panW)
								  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
								  .addComponent(button8, GroupLayout.PREFERRED_SIZE, 52, GroupLayout.PREFERRED_SIZE)
								  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
								  .addComponent(panE)
								  .addGap(13, 13, 13)
								  .addComponent(btnQuit))
							  .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
								  .addComponent(comboBox1, GroupLayout.Alignment.LEADING, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								  .addGroup(GroupLayout.Alignment.LEADING, panel1Layout.createSequentialGroup()
									  .addComponent(panSW)
									  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
									  .addComponent(panS)
									  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
									  .addComponent(panSE))))
						  .addGap(54, 54, 54))
			  );
			  panel1Layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {button8, panE, panN, panNE, panNW, panS, panSE, panSW, panW});
			  panel1Layout.setVerticalGroup(
				  panel1Layout.createParallelGroup()
					  .addGroup(panel1Layout.createSequentialGroup()
						  .addGroup(panel1Layout.createParallelGroup()
							  .addGroup(panel1Layout.createSequentialGroup()
								  .addGap(4, 4, 4)
								  .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
									  .addComponent(label5)
									  .addComponent(ttfLon, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
							  .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
								  .addComponent(panN)
								  .addComponent(panNW)
								  .addComponent(panNE)))
						  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						  .addGroup(panel1Layout.createParallelGroup()
							  .addGroup(panel1Layout.createSequentialGroup()
								  .addGroup(panel1Layout.createParallelGroup()
									  .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
										  .addComponent(button8)
										  .addComponent(panW)
										  .addComponent(panE))
									  .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
										  .addComponent(label4)
										  .addComponent(ttfLat, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
								  .addGroup(panel1Layout.createParallelGroup()
									  .addGroup(panel1Layout.createSequentialGroup()
										  .addGap(5, 5, 5)
										  .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
											  .addComponent(panSE)
											  .addComponent(panS)
											  .addComponent(panSW)
											  .addComponent(ttfZoom, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
									  .addGroup(panel1Layout.createSequentialGroup()
										  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
										  .addComponent(label1)))
								  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
								  .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
									  .addComponent(slider1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									  .addComponent(comboBox1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
							  .addComponent(btnQuit))
						  .addGap(15, 15, 15))
			  );
			  panel1Layout.linkSize(SwingConstants.VERTICAL, new Component[] {button8, panE, panN, panNE, panNW, panS, panSE, panSW, panW});
		  }
		  contentPanel.add(panel1, new TableLayoutConstraints(0, 0, 0, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

		  //======== scrollPane1 ========
		  {
			  scrollPane1.setBorder(new TitledBorder("System.out - displays all status and progress messages, etc."));
			  scrollPane1.setOpaque(false);

			  //---- ttaStatus ----
			  ttaStatus.setBorder(Borders.createEmptyBorder("1dlu, 1dlu, 1dlu, 1dlu"));
			  ttaStatus.setToolTipText("<html>Task progress updates (messages) are displayed here,<br>along with any other output generated by the Task.<html>");
			  scrollPane1.setViewportView(ttaStatus);
		  }
		  contentPanel.add(scrollPane1, new TableLayoutConstraints(0, 1, 0, 1, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

		  //======== panel2 ========
		  {
			  panel2.setOpaque(false);
			  panel2.setBorder(new CompoundBorder(
				  new TitledBorder("Status - control progress reporting"),
				  Borders.DLU2_BORDER));
			  panel2.setLayout(new TableLayout(new double[][] {
				  {0.45, TableLayout.FILL, 0.45},
				  {TableLayout.PREFERRED, TableLayout.PREFERRED}}));
			  ((TableLayout)panel2.getLayout()).setHGap(5);
			  ((TableLayout)panel2.getLayout()).setVGap(5);

			  //======== panel3 ========
			  {
				  panel3.setOpaque(false);
				  panel3.setLayout(new GridLayout(1, 2));

				  //---- checkboxRecvStatus ----
				  checkboxRecvStatus.setText("Enable \"Recieve\"");
				  checkboxRecvStatus.setOpaque(false);
				  checkboxRecvStatus.setToolTipText("Task will fire \"send\" status updates");
				  checkboxRecvStatus.setSelected(true);
				  panel3.add(checkboxRecvStatus);

				  //---- checkboxSendStatus ----
				  checkboxSendStatus.setText("Enable \"Send\"");
				  checkboxSendStatus.setOpaque(false);
				  checkboxSendStatus.setToolTipText("Task will fire \"recieve\" status updates");
				  panel3.add(checkboxSendStatus);
			  }
			  panel2.add(panel3, new TableLayoutConstraints(0, 0, 0, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

			  //---- ttfProgressMsg ----
			  ttfProgressMsg.setText("Loading map from Google Static Maps");
			  ttfProgressMsg.setToolTipText("Set the task progress message here");
			  panel2.add(ttfProgressMsg, new TableLayoutConstraints(2, 0, 2, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

			  //---- progressBar ----
			  progressBar.setStringPainted(true);
			  progressBar.setString("progress %");
			  progressBar.setToolTipText("% progress is displayed here");
			  panel2.add(progressBar, new TableLayoutConstraints(0, 1, 0, 1, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

			  //---- lblProgressStatus ----
			  lblProgressStatus.setText("task status listener");
			  lblProgressStatus.setHorizontalTextPosition(SwingConstants.LEFT);
			  lblProgressStatus.setHorizontalAlignment(SwingConstants.LEFT);
			  lblProgressStatus.setToolTipText("Task status messages are displayed here when the task runs");
			  panel2.add(lblProgressStatus, new TableLayoutConstraints(2, 1, 2, 1, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
		  }
		  contentPanel.add(panel2, new TableLayoutConstraints(0, 2, 0, 2, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
	  }
	  dialogPane.add(contentPanel, BorderLayout.CENTER);

	  //---- PictureBox ----
	  PictureBox.setText("Configure Your Settings and Press \"GO\" to view a map. ");
	  PictureBox.setMaximumSize(new Dimension(90, 90));
	  PictureBox.setMinimumSize(new Dimension(20, 20));
	  PictureBox.setPreferredSize(new Dimension(512, 512));
	  PictureBox.setHorizontalAlignment(SwingConstants.CENTER);
	  dialogPane.add(PictureBox, BorderLayout.WEST);
  }
  contentPane.add(dialogPane, BorderLayout.CENTER);
  setSize(1150, 485);
  setLocationRelativeTo(null);

  //---- bindings ----
  bindingGroup = new BindingGroup();
  bindingGroup.bind();
  // JFormDesigner - End of component initialization  //GEN-END:initComponents
}

// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
// Generated using JFormDesigner Evaluation license - Billy Thorton
private JPanel dialogPane;
private JPanel contentPanel;
private JPanel panel1;
private JLabel label5;
private JTextField ttfLon;
private JButton panNE;
private JButton panN;
private JButton panNW;
private JLabel label4;
private JTextField ttfLat;
private JButton panW;
private JButton button8;
private JButton panE;
private JButton btnQuit;
private JLabel label1;
private JTextField ttfZoom;
private JButton panSW;
private JButton panS;
private JButton panSE;
private JSlider slider1;
private JComboBox comboBox1;
private JScrollPane scrollPane1;
private JTextArea ttaStatus;
private JPanel panel2;
private JPanel panel3;
private JCheckBox checkboxRecvStatus;
private JCheckBox checkboxSendStatus;
private JTextField ttfProgressMsg;
private JProgressBar progressBar;
private JLabel lblProgressStatus;
private JLabel PictureBox;
private BindingGroup bindingGroup;
// JFormDesigner - End of variables declaration  //GEN-END:variables
}
