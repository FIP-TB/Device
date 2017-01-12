package deviceName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.control.ActionListener;
import org.cybergarage.upnp.control.QueryListener;
import org.cybergarage.upnp.device.InvalidDescriptionException;

public class ArduinoLampe extends Device implements ActionListener, QueryListener, SerialPortEventListener{ // Implement 

	private BufferedReader sInput; // Reads from an input stream
	private static OutputStream sOutput; // Used for writing out to arduino (not used right now)
	private SerialPort serialPort; // Similar to a socket. Allows for ardruino-PC exchange.
	private static final String PORTS[] = {
		"/dev/tty.usbmodem", // Port on Mac OS X
		"COM3" // Port for Windows
	};

	private static final int TIME_OUT = 2000; // 2 seconds
	private static final int DATA_RATE = 9600; // Arduino communication rate
	String target = "0";
	String status = "0";
	String automatic = "1";
	String sensibility = "15";

	public ArduinoLampe(String description) throws InvalidDescriptionException {
		super (description);
		Action setSensAction = getAction("SetSensibility");
		setSensAction.setActionListener(this);
		Action getSensAction = getAction("GetSensibility");
		getSensAction.setActionListener(this); 
		Action setAutoAction = getAction("SetAutomatic");
		setAutoAction.setActionListener(this);
		Action getAutoAction = getAction("GetAutomatic");
		getAutoAction.setActionListener(this); 
		Action setTimeAction = getAction("SetTarget");
		setTimeAction.setActionListener(this);
		Action getTimeAction = getAction("GetTarget");
		getTimeAction.setActionListener(this); 
		Action getStatusAction = getAction("GetStatus");
		getStatusAction.setActionListener(this); 
		StateVariable stateTarget = getStateVariable("Target");
		stateTarget.setQueryListener(this); 
		StateVariable stateStatus = getStateVariable("Status");
		stateStatus.setQueryListener(this); 
		StateVariable stateAutomatic = getStateVariable("Automatic");
		stateAutomatic.setQueryListener(this); 
		StateVariable stateSensibility = getStateVariable("Sensibility");
		stateSensibility.setQueryListener(this); 

		this.initialize(); // Make sure we set up our serial port connection
		System.out.println("Listening for arduino...");
	}

	@Override
	public boolean queryControlReceived(StateVariable stateVar) {
		if (stateVar.equals("Target") == true) { 
			String currTargetStr = "stateVar";
			stateVar.setValue(currTargetStr);
			return true; 
		}
		stateVar.setStatus(400, "Invalid var"); 
		return false;
	}

	@Override
	public boolean actionControlReceived(Action action) {
		ArgumentList argList = action.getArgumentList(); 
		String actionName = action.getName();
		System.out.println(actionName);

		//Si l'utilisateur demande d'affecter un état à la lampe
		if (actionName.equals("SetSensibility") == true) {
			Argument inSensibility = argList.getArgument("sensibility");
			String sensibilityValue = inSensibility.getValue();
			if (sensibilityValue == null || sensibilityValue.length() <= 0)
				return false;
			sensibility = sensibilityValue;
			this.writeData("~S"+sensibilityValue+"-");
			return true;
		}
		//Si l'utilisateur demande si la lampe est en auto ou non
		else if (actionName.equals("GetSensibility") == true) {
			String currSenStr = sensibility ;
			Argument currSenArg = argList.getArgument("retSensibility");
			currSenArg.setValue(currSenStr);
			return true;
		}
		else if (actionName.equals("SetAutomatic") == true) {
			Argument inAutomatic = argList.getArgument("automatic");
			String automaticValue = inAutomatic.getValue();
			if (automaticValue == null || automaticValue.length() <= 0)
				return false;
			automatic = automaticValue;
			this.writeData("~A"+automaticValue+"-");
			return true;
		}
		//Si l'utilisateur demande si la lampe est en auto ou non
		else if (actionName.equals("GetAutomatic") == true) {
			String currAutoStr = automatic ;
			Argument currAutoArg = argList.getArgument("RetAutomaticValue");
			currAutoArg.setValue(currAutoStr);
			return true;
		}
		else if (actionName.equals("SetTarget") == true) {
			Argument inTarget = argList.getArgument("newTargetValue");
			String targetValue = inTarget.getValue();
			if (targetValue == null || targetValue.length() <= 0)
				return false;
			target = targetValue;
			this.writeData("~T"+targetValue+"-");
			return true;
		}
		//Si l'utilisateur demande l'état de la lampe
		else if (actionName.equals("GetTarget") == true) {
			String currTargetStr = target ;
			Argument currTargetArg = argList.getArgument("RetTargetValue");
			currTargetArg.setValue(currTargetStr);
			return true;
		}
		//Si l'utilisateur demande la valeur du capteur de luminosité
		else if (actionName.equals("GetStatus") == true) {

			String currStatusStr = status ;
			Argument currStatusArg = argList.getArgument("ResultStatus");
			currStatusArg.setValue(currStatusStr); 
			return true;
		}
		action.setStatus(400, "Invalid action"); return false;
	}

	//----------------Partie communication arduino-----------------
	private void initialize() { // Method we will call to setup serial connection
		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers(); // For enumerating through 'ports' on computer

		while (portEnum.hasMoreElements() && portId == null) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : PORTS) {
				if (currPortId.getName().contains(portName)) { // If it detects a valid arduino com port...
					System.out.println("Arduino found on port: " + currPortId.getName());
					portId = currPortId; // Set port object to use with serial object later.
				}
			}
		}

		if (portId == null) { // If no arduino com port was found. Is your arduino plugged in?
			System.out.println("Error: No COM port found");
			return;
		}

		try {
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT); // Attempt to open the com port
			serialPort.setSerialPortParams(DATA_RATE, serialPort.DATABITS_8,
					serialPort.STOPBITS_1, serialPort.PARITY_NONE); // Set comm specs for the arduino-pc port we created
			sInput = new BufferedReader(new InputStreamReader(serialPort.getInputStream())); // Set our buffered reader to read from the serial comm port.
			sOutput = serialPort.getOutputStream();

			serialPort.addEventListener(this); // Set the class to to respond to an event when serialPort gets data
			serialPort.notifyOnDataAvailable(true); // This is the 'event' we are looking for! (when data is sent down the pipe)
		} catch (PortInUseException e) {
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}

	}

	public synchronized void closeCommPort() { // Shut down the port when we are done
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	public synchronized void writeData(String data) {
		try {
			sOutput.write(data.getBytes());
			sOutput.flush();
		} catch (Exception e) {
			System.out.println("could not write to port");
		}
	}


	@Override
	public void serialEvent(SerialPortEvent event) { // Required method for implementing the event listener
		if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				String receivedInput = sInput.readLine(); // Read buffered input into a string to print
				//System.out.println(receivedInput);
				if(receivedInput.startsWith("target")){
					target = receivedInput.substring(6);
				}else if(receivedInput.startsWith("Commande: A")){
					automatic = receivedInput.substring(11);
				}else if(receivedInput.startsWith("Commande: T")){
					target = receivedInput.substring(11);
				}else if(receivedInput.startsWith("Commande: S")){
					sensibility = receivedInput.substring(11);
				}else if(Integer.parseInt(receivedInput) != 0){
					status = receivedInput;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}