package DistributedLocking;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
public class Server extends Thread {
	private Integer Id;
	private Utility util;
	public Server(int id,Utility util){
		  super();
		  this.Id = id ; 
		  this.util = util;
	}
	public void run() {
		System.out.println("Id: "+this.Id);
		try {
			System.out.println("Server instanciation "+ this.Id);
			RemoteMethodInterfaces Service = new RemoteMethodImplementaion(this.Id,this.util);
			Registry registry = LocateRegistry.getRegistry();
            registry.rebind(Integer.toString(this.Id), Service);
			//Naming.rebind(Integer.toString(this.Id),Service);
		}
		catch(Exception e){
			System.out.println("rebind"+e.toString());
			e.printStackTrace();
		}
	}
}
