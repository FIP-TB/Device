package deviceName;
import org.cybergarage.upnp.*;
import org.cybergarage.upnp.device.*;

public class DeviceTest {

	public static void printDevice(Device dev) {
		String devName = dev.getFriendlyName(); 
		System.out.println(devName);
		DeviceList childDevList = dev.getDeviceList(); 
		int nChildDevs = childDevList.size();
		for (int n =0; n <nChildDevs; n ++) {
			Device childDev = childDevList.getDevice(n); 
			printDevice(childDev);
		} 
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String descriptionFileName = "device/description/description.xml";
		ArduinoLampe upnpDev = null;
		try{
			upnpDev = new ArduinoLampe(descriptionFileName);
			upnpDev.start(); 
		}
		catch (InvalidDescriptionException e){ 
			String errMsg = e.getMessage();
			System.out.println("InvalidDescriptionException = " + errMsg);
		}
	}
}
