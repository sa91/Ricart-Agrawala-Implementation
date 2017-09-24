package DistributedLocking;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.TreeMap;
import DistributedLocking.Messageproto.Message;
import com.google.protobuf.InvalidProtocolBufferException;

public class RemoteMethodImplementaion extends UnicastRemoteObject implements
		RemoteMethodInterfaces {

	//declarations	 
	private Integer Id;
	private Utility util;
		
	//Constructor
	public RemoteMethodImplementaion(int id,Utility util) throws RemoteException {
	       super();
	       this.Id = id ; 
	       this.util = util;
	       System.out.println("RemotemethodImp: "+this.Id);
	}	
	
	
	public byte[] LockRequest(byte[] msg) {
		Message request=null;
		try {
			request = Message.parseFrom(msg);
		} catch (InvalidProtocolBufferException e) {
			System.out.println("Server:LockRequest message parse:"+e);
			e.printStackTrace();
		}
		TreeMap <Integer,Integer> MsgClock = this.util.CreateClock(request);
		Date date = new Date(); 
		System.out.println("Server:LockRequest: "+ date.getTime() +" Myid: "+this.Id+" senderid: "+ request.getProcessID() + " sendersClock: "+ MsgClock.toString()); //+ " myClock: "+ this.util.Clock.toString());
		if(this.util.getState() != 0)
		{
			if(this.util.IsGreater(MsgClock)>0) {
				this.util.insertWaiter(request.getProcessID());
				System.out.println("Server:LockRequestx: State: "+util.getState()+" Myid: "+this.Id+" senderid: "+ request.getProcessID() + " reply:OK");
				return util.CreateMessage("OK",this.Id,1).build().toByteArray();
			}
			else{
				System.out.println("Server:LockRequesty: State: "+util.getState()+" Myid: "+this.Id+" senderid: "+ request.getProcessID() + " reply:ACK");
				return util.CreateMessage("ACK",this.Id,1).build().toByteArray();
			}
		}
		System.out.println("Server:LockRequestz : State: "+util.getState()+" Myid: "+this.Id+" senderid: "+ request.getProcessID() + " reply:ACK");
		return util.CreateMessage("ACK",this.Id,1).build().toByteArray();
	}

	public byte[] UnlockRequest(byte[] msg)  {
		Message request=null;
		try {
			request = Message.parseFrom(msg);
		} catch (InvalidProtocolBufferException e) {
			System.out.println("Server:UnLockRequest message parse:"+e);
			e.printStackTrace();
		}
		TreeMap <Integer,Integer> MsgClock = this.util.CreateClock(request);
		Date date = new Date(); 
		System.out.println("Server:UnLockRequest: "+ date.getTime() +" Myid: "+this.Id+" senderid: "+ request.getProcessID() + " sendersClock: "+ MsgClock.toString()); //+ " myClock: "+ this.util.Clock.toString());
		this.util.RecvAcknowledge(request.getProcessID(),this.Id);
		return util.CreateMessage("DONE",this.Id,1).build().toByteArray();
	}
}
