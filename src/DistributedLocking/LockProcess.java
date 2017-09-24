package DistributedLocking;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Date;

import DistributedLocking.Messageproto.Message;

import com.google.protobuf.InvalidProtocolBufferException;

public class LockProcess {

	public static void main(String[] args) throws RemoteException {
		System.out.println("Hello");
		final int LIMIT = 100;		
		//commandline Arguments
		if(args.length != 6){
			System.out.println("Number of arguments are Incorrect "+ args.length);
			for(String s:args)
				System.out.println(s);
		}
		int myId = Integer.parseInt(args[1]);
		int NumberofProcesses = Integer.parseInt(args[3]);
		String Outfile = args[5];
		System.out.println("mayid: "+ myId +" NumberofProc: "+NumberofProcesses+" file: "+Outfile);	
		
		//Instantiate Utilities;
		Utility Util =  new Utility(Outfile,NumberofProcesses);
		Util.Clock.put(myId, 1);
		
		//Start the Server;
		Server ServerThread = new Server(myId,Util);
		ServerThread.start();
		
		//Get Handles
		RemoteMethodInterfaces [] Handle = new RemoteMethodInterfaces[NumberofProcesses+5]; 
		if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
		for(int i=1;i<=NumberofProcesses;i++)
		{
			if(i==myId) continue;
			while(true){
				try{
					Registry registry = LocateRegistry.getRegistry();
					Handle[i]= (RemoteMethodInterfaces) registry.lookup(Integer.toString(i));
					System.out.println("Get Handle:Got handle of "+i+" myid: "+myId);
					break;
				} catch (AccessException e) {
					System.out.println("Get Handle:Access Exception");
					e.printStackTrace();
				} catch (RemoteException e) {
					System.out.println("Get Handle:Remote Exception");
					e.printStackTrace();
				} catch (NotBoundException e) {
					System.out.println("Get Handle:Not Bound,slepping for 100ms");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						System.out.println("Get Handle:Can't sleep");
						//e1.printStackTrace();
					}
					//e.printStackTrace();
				}
				catch (Exception e) {
					System.out.println("Get Handle: Exception "+ e);
					e.printStackTrace();
				}			
			}
		}
		while(true){
			//lock request
			Util.Interested(myId);
			for(int i=1;i<= NumberofProcesses;i++)
				if(i!=myId)
				{
					byte [] msg = Handle[i].LockRequest(Util.CreateMessage("Lock", myId, 0).build().toByteArray());
					Message response =null;
					try {
						response = Message.parseFrom(msg);
						Util.CreateClock(response);
					} catch (InvalidProtocolBufferException e) {
						System.out.println("Client:Lock requesting: "+ e );
						e.printStackTrace();
					}
					Date date = new Date();
					System.out.println("Client:Lock call: "+date.toString()+":: myid: "+ myId +" reciverid: "+ i+" RequestClock: "+ Util.RequestClock.toString());// + "MyClock: "+ Util.Clock.toString());
					if(response.getName().equals("ACK")){
						System.out.println("Client:Lock call got ACK: "+date.getTime()+":: myid: "+ myId +" senders id: "+ i );//" MyClock: "+ Util.Clock.toString());
						Util.RecvAcknowledge(response.getProcessID(),myId);
					}
					else 
						System.out.println("Client:Lock call got OK:: myid:"+ myId +" senders id: "+ i);
				}	 
			if(Util.write(myId,LIMIT) == LIMIT) break;
			ArrayList <Integer> Waiters = Util.NotIntereseted(myId);
			System.out.println("Client:<<<<<< length of waiters"+ Waiters.size()+ " >>>>>>>>");
			for(Integer val:Waiters)
			{
				byte[] newmsg = Handle[val].UnlockRequest(Util.CreateMessage("ACK", myId, 1).build().toByteArray());
				Date newdate = new Date();
				System.out.println("Client:UnLock call:"+ newdate.getTime() +":: myid: "+ myId +" reciverid: "+ val);//+ " MyClock: "+ Util.Clock.toString());
				Message response;
				try {
					response = Message.parseFrom(newmsg);
					Util.CreateClock(response);
				} catch (InvalidProtocolBufferException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
			System.out.println("Client:Done writting,sleeping for 100ms");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				System.out.println("Client:Can't sleep after writing too");
				e.printStackTrace();
			}
		}
	}	
}
