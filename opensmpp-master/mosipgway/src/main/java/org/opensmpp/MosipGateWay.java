package org.opensmpp;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import org.smpp.Data;
import org.smpp.Session;
import org.smpp.TCPIPConnection;
import org.smpp.pdu.*;
import org.smpp.util.DataCodingCharsetHandler;

public class MosipGateWay {


    /**
     * File with default settings for the application.
     */
    static String propsFilePath = "C:\\Users\\Hansal\\Downloads\\mosip\\opensmpp-master\\client\\smppsender.cfg";

    /**
     * This is the SMPP session used for communication with SMSC.
     */
    static Session session = null;

    /**
     * Contains the parameters and default values for this test
     * application such as system id, password, default npi and ton
     * of sender etc.
     */
    Properties properties = new Properties();

    /**
     * If the application is bound to the SMSC.
     */
    boolean bound = false;

    /**
     * Address of the SMSC.
     */
    String ipAddress = null;

    /**
     * The port number to bind to on the SMSC server.
     */
    int port = 0;

    /**
     * The name which identifies you to SMSC.
     */
    String systemId = null;

    /**
     * The password for authentication to SMSC.
     */
    String password = null;

    /**
     * How you want to bind to the SMSC: transmitter (t), receiver (r) or
     * transciever (tr). Transciever can both send messages and receive
     * messages. Note, that if you bind as receiver you can still receive
     * responses to you requests (submissions).
     */
    String bindOption = "tr";

    /**
     * The range of addresses the smpp session will serve.
     */
    AddressRange addressRange = new AddressRange();

    /*
     * for information about these variables have a look in SMPP 3.4
     * specification
     */
    String systemType = "";
    String serviceType = "";
    Address sourceAddress = new Address();
    Address destAddress = new Address();
    String scheduleDeliveryTime = "";
    String validityPeriod = "";
    String shortMessage = "";
    int numberOfDestination = 1;
    String messageId = "";
    byte esmClass = 0;
    byte protocolId = 0;
    byte priorityFlag = 0;
    byte registeredDelivery = 0;
    byte replaceIfPresentFlag = 0;
    byte dataCoding = 0;
    byte smDefaultMsgId = 0;

    /**
     * If you attemt to receive message, how long will the application
     * wait for data.
     */
    long receiveTimeout = Data.RECEIVE_BLOCKING;

    public MosipGateWay() throws IOException{
        loadProperties(propsFilePath);
    }

    public static void main(String[] args) {

        String sender = null;
        byte senderTon = (byte) 0;
        byte senderNpi = (byte) 0;
        String dest = null;
        String message = null;


        System.out.println("Initialising...");
        MosipGateWay mosipGateWay = null;
        try {
            mosipGateWay = new MosipGateWay();
        } catch (IOException e) {
            System.out.println("Exception initialising SMPPSender " + e);
        }

        if (mosipGateWay != null) {
            mosipGateWay.bind();

            if(mosipGateWay.bound) {

                Scanner scanner = new Scanner(System.in);
                try {
                    while (true) {
//                        System.out.println("Enter Message : ");
//                        message = scanner.nextLine();
//                        if (message.equals("quit"))
//                            mosipGateWay.unbind();

                        PDU pdu = session.receive(150000);

                        if(pdu != null){
                            DeliverSM sms = (DeliverSM) pdu;
                            System.out.println("***** New Message Received *****");
                            System.out.println("---- > "+sms.getShortMessage());
//                            mosipGateWay.submit(de, message, "mosipgway", senderTon, senderNpi);
                        }else{
                            System.out.println("No message");
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else{
            System.out.println("Not estb. !!!!");
            }
        }


    }

    private void getCredentials(){

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter username ::  ");
        this.systemId =  "mosipgway";//scanner.nextLine();
        System.out.println("Enter password ::  ");
        this.password = "12345";//scanner.nextLine();
        System.out.println("Enter your mobile no :: ");
//        String addrs = scanner.nextLine();
//        try {
//            this.sourceAddress.setAddress(addrs);
//        }catch (Exception e){}
    }

    private void bind() {
        try {
            if (bound) {
                System.out.println("Already bound, unbind first.");
                return;
            }

            BindRequest request = null;
            BindResponse response = null;

            request = new BindTransmitter();

            TCPIPConnection connection = new TCPIPConnection(ipAddress, port);
            connection.setReceiveTimeout(20 * 1000);
            session = new Session(connection);

            getCredentials();

            // set values
            request.setSystemId(systemId);
            request.setPassword(password);
            request.setSystemType(systemType);
            request.setInterfaceVersion((byte) 0x34);
            request.setAddressRange(addressRange);

            // send the request
            System.out.println("Bind request " + request.debugString());
            response = session.bind(request);
            System.out.println("Bind response " + response.debugString());
            if (response.getCommandStatus() == Data.ESME_ROK) {
                bound = true;
            } else {
                System.out.println("Bind failed, code " + response.getCommandStatus());
            }
        } catch (Exception e) {
            System.out.println("Bind operation failed. " + e);
        }
    }

    private void unbind() {
        try {

            if (!bound) {
                System.out.println("Not bound, cannot unbind.");
                return;
            }

            // send the request
            System.out.println("Going to unbind.");
            if (session.getReceiver().isReceiver()) {
                System.out.println("It can take a while to stop the receiver.");
            }
            UnbindResp response = session.unbind();
            System.out.println("Unbind response " + response.debugString());
            bound = false;
        } catch (Exception e) {
            System.out.println("Unbind operation failed. " + e);
        }
    }

    private void submit(String destAddress, String shortMessage, String sender, byte senderTon, byte senderNpi) {
        try {
            SubmitSM request = new SubmitSM();
            SubmitSMResp response;

            // set values
            request.setServiceType(serviceType);

            if(sender != null) {
                if(sender.startsWith("+")) {
                    sender = sender.substring(1);
                    senderTon = 1;
                    senderNpi = 1;
                }
                if(!sender.matches("\\d+")) {
                    senderTon = 5;
                    senderNpi = 0;
                }

                if(senderTon == 5) {
                    request.setSourceAddr(new Address(senderTon, senderNpi, sender, 11));
                } else {
                    request.setSourceAddr(new Address(senderTon, senderNpi, sender));
                }
            } else {
                request.setSourceAddr(sourceAddress);
            }

            if(destAddress.startsWith("+")) {
                destAddress = destAddress.substring(1);
            }
            request.setDestAddr(new Address((byte)1, (byte)1, destAddress));
            request.setReplaceIfPresentFlag(replaceIfPresentFlag);

            String encoding = DataCodingCharsetHandler.getCharsetName(dataCoding);
            request.setShortMessage(shortMessage, encoding);
            request.setScheduleDeliveryTime(scheduleDeliveryTime);
            request.setValidityPeriod(validityPeriod);
            request.setEsmClass(esmClass);
            request.setProtocolId(protocolId);
            request.setPriorityFlag(priorityFlag);
            request.setRegisteredDelivery(registeredDelivery);
            request.setDataCoding(dataCoding);
            request.setSmDefaultMsgId(smDefaultMsgId);

            // send the request

            request.assignSequenceNumber(true);
            System.out.println("Submit request " + request.debugString());
            response = session.submit(request);
            System.out.println("Submit response " + response.debugString());
            messageId = response.getMessageId();
            enquireLink();
        } catch (Exception e) {
            System.out.println("Submit operation failed. " + e);
        }
    }


    private void enquireLink() {
        try {
            EnquireLink request = new EnquireLink();
            EnquireLinkResp response;
            System.out.println("Enquire Link request " + request.debugString());
            response = session.enquireLink(request);
            System.out.println("Enquire Link response " + response.debugString());
        } catch (Exception e) {
            System.out.println("Enquire Link operation failed. " + e);
        }
    }

    private void loadProperties(String fileName) throws IOException {
        System.out.println("Reading configuration file " + fileName + "...");
        FileInputStream propsFile = new FileInputStream(fileName);
        properties.load(propsFile);
        propsFile.close();
        System.out.println("Setting default parameters...");
        byte ton;
        byte npi;
        String addr;
        String bindMode;
        int rcvTimeout;

        ipAddress = properties.getProperty("ip-address");
        port = getIntProperty("port", port);
        systemId = properties.getProperty("system-id");
        password = properties.getProperty("password");

        ton = getByteProperty("addr-ton", addressRange.getTon());
        npi = getByteProperty("addr-npi", addressRange.getNpi());
        addr = properties.getProperty("address-range", addressRange.getAddressRange());
        addressRange.setTon(ton);
        addressRange.setNpi(npi);
        try {
            addressRange.setAddressRange(addr);
        } catch (WrongLengthOfStringException e) {
            System.out.println("The length of address-range parameter is wrong.");
        }

        ton = getByteProperty("source-ton", sourceAddress.getTon());
        npi = getByteProperty("source-npi", sourceAddress.getNpi());
        addr = properties.getProperty("source-address", sourceAddress.getAddress());
        setAddressParameter("source-address", sourceAddress, ton, npi, addr);

        ton = getByteProperty("destination-ton", destAddress.getTon());
        npi = getByteProperty("destination-npi", destAddress.getNpi());
        addr = properties.getProperty("destination-address", destAddress.getAddress());
        setAddressParameter("destination-address", destAddress, ton, npi, addr);

        serviceType = properties.getProperty("service-type", serviceType);
        systemType = properties.getProperty("system-type", systemType);
        bindMode = properties.getProperty("bind-mode", bindOption);
        if (bindMode.equalsIgnoreCase("transmitter")) {
            bindMode = "t";
        } else if (bindMode.equalsIgnoreCase("receiver")) {
            bindMode = "r";
        } else if (bindMode.equalsIgnoreCase("transciever")) {
            bindMode = "tr";
        } else if (
                !bindMode.equalsIgnoreCase("t") && !bindMode.equalsIgnoreCase("r") && !bindMode.equalsIgnoreCase("tr")) {
            System.out.println(
                    "The value of bind-mode parameter in "
                            + "the configuration file "
                            + fileName
                            + " is wrong. "
                            + "Setting the default");
            bindMode = "t";
        }
        bindOption = bindMode;

        // receive timeout in the cfg file is in seconds, we need milliseconds
        // also conversion from -1 which indicates infinite blocking
        // in the cfg file to Data.RECEIVE_BLOCKING which indicates infinite
        // blocking in the library is needed.
        if (receiveTimeout == Data.RECEIVE_BLOCKING) {
            rcvTimeout = -1;
        } else {
            rcvTimeout = ((int) receiveTimeout) / 1000;
        }
        rcvTimeout = getIntProperty("receive-timeout", rcvTimeout);
        if (rcvTimeout == -1) {
            receiveTimeout = Data.RECEIVE_BLOCKING;
        } else {
            receiveTimeout = rcvTimeout * 1000;
        }
    }

    /**
     * Gets a property and converts it into byte.
     */
    private byte getByteProperty(String propName, byte defaultValue) {
        return Byte.parseByte(properties.getProperty(propName, Byte.toString(defaultValue)));
    }

    /**
     * Gets a property and converts it into integer.
     */
    private int getIntProperty(String propName, int defaultValue) {
        return Integer.parseInt(properties.getProperty(propName, Integer.toString(defaultValue)));
    }

    /**
     * Sets attributes of <code>Address</code> to the provided values.
     */
    private void setAddressParameter(String descr, Address address, byte ton, byte npi, String addr) {
        address.setTon(ton);
        address.setNpi(npi);
        try {
            address.setAddress(addr);
        } catch (WrongLengthOfStringException e) {
            System.out.println("The length of " + descr + " parameter is wrong.");
        }
    }

}