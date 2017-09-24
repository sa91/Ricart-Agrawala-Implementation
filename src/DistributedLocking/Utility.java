package DistributedLocking;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import DistributedLocking.Messageproto.Message;
public class Utility {
	//declarations
	
	private String fileName ;
	private volatile Integer State;  // 0:Not-Interested,1:Interested,2:Holding
	private Integer NumberofAcks;
	private Integer NumberofProcess;
	
	//synchronise
	private final Lock lock;  // For condition
	private final Lock TotalLock;
	private final Condition Ready;
	
	//DataStructures
	private Integer[] RecievedAck;
	public TreeMap<Integer,Integer>  Clock ;
	public TreeMap<Integer,Integer>  RequestClock ;
	
	private  ArrayList<Integer>  WaitingList; 
	
	public Utility(String file,int Nproc){
		this.fileName = file;
		this.State = 0;
		this.NumberofProcess = Nproc;
		this.Clock = new TreeMap<Integer,Integer>();
		this.RecievedAck = new Integer[this.NumberofProcess+5];
		this.lock = new ReentrantLock();
		this.TotalLock = new ReentrantLock();
		this.Ready = this.TotalLock.newCondition();
		
	}
	
	//clock_comparison
	public int IsGreater(TreeMap<Integer,Integer> CX) {
		this.TotalLock.lock();
		TreeMap <Integer,Integer> C2 = new TreeMap<Integer,Integer>(this.RequestClock);
		this.TotalLock.unlock();
		TreeMap <Integer,Integer> C1 = new TreeMap<Integer,Integer>(CX);
		while(!C1.isEmpty() && !C2.isEmpty()){
			Entry<Integer, Integer> P1 = C1.firstEntry(),P2 = C2.firstEntry();
			C1.remove(P1.getKey());C2.remove(P2.getKey());
			if(P1.getKey()==P2.getKey()) {
				if(P1.getValue()== P2.getValue()) continue;
				System.out.println("Utility:Diffin value:"+(P1.getValue()+" "+ P2.getValue()));
				return P1.getValue() - P2.getValue();
			}else {
				System.out.println("Utility:Diffin key:"+(P1.getKey()+" "+ P2.getKey()));
				return P2.getKey()-P1.getKey();        		
			}
		}
		if(C2.isEmpty()) return 1;
		return -1;
	}
	//update_Clock
	public  void UpdateClock(TreeMap<Integer,Integer> CMSG){
		this.TotalLock.lock();
		for(Map.Entry<Integer,Integer> entry: CMSG.entrySet()){
			if(this.Clock.containsKey(entry.getKey()))
				this.Clock.put(entry.getKey(), Math.max(entry.getValue(),this.Clock.get(entry.getKey())));
			else
				this.Clock.put(entry.getKey(),entry.getValue());
		}
		System.out.println("Utility:updated Clock:"+this.Clock.toString());
		this.TotalLock.unlock();
	}
	// build message
	Message.Builder CreateMessage(String Msg,int Id,int type)
	{
		Message.Builder message = Message.newBuilder();
		message.setName(Msg);
		message.setProcessID(Id);
		TreeMap <Integer,Integer> tosendClock;
		if(type==1){
			this.TotalLock.lock();
			this.Clock.put(Id,this.Clock.get(Id)+1);
			tosendClock = new TreeMap<Integer,Integer>(this.Clock);
			this.TotalLock.unlock();
		}
		else{
			this.TotalLock.lock();
			tosendClock = new TreeMap<Integer,Integer>(this.RequestClock);
			this.TotalLock.unlock();
		}		
		for(Map.Entry<Integer, Integer> entry:tosendClock.entrySet())
		{
			Message.ClockPair.Builder Cpair = Message.ClockPair.newBuilder();
			Cpair.setProcessNumber(entry.getKey());
			Cpair.setCounter(entry.getValue());
			message.addPair(Cpair);
		}
		return message;
	}
	// parse Clock
	TreeMap<Integer,Integer> CreateClock(Message Msg)
	{
		TreeMap<Integer,Integer> NewClock = new TreeMap<Integer,Integer>();
		for(Message.ClockPair Cpair:Msg.getPairList())
			NewClock.put(Cpair.getProcessNumber(),Cpair.getCounter());
		this.UpdateClock(NewClock);
		return NewClock;
	}	
	
	public void Lockheld(){
		this.TotalLock.lock();
		this.State = 2;
		this.Ready.signal();
		this.TotalLock.unlock();
	}
	public void Interested(int Id){
		if(this.State == 0){
			this.TotalLock.lock();
			this.Clock.put(Id,this.Clock.get(Id)+1);
			this.State= 1;
			this.RequestClock = new TreeMap<Integer,Integer>(this.Clock);
			System.out.println("Utility:Interested clock: "+ this.Clock.toString());
			this.NumberofAcks = 0;
			for(int i=0;i<=NumberofProcess;i++) this.RecievedAck[i]=0;
			this.WaitingList = new ArrayList <Integer>();
			this.TotalLock.unlock();
		}  
	}
	public ArrayList<Integer> NotIntereseted(int Id){
		this.TotalLock.lock();
		this.State = 0;
		this.Clock.put(Id,this.Clock.get(Id)+1);
		ArrayList <Integer> ret = new ArrayList <Integer> (this.WaitingList); 
		this.TotalLock.unlock();
		return ret;
	}
	public int getState(){
		this.TotalLock.lock();
		int val=this.State;
		this.TotalLock.unlock();
		return val;
	}
	public void RecvAcknowledge(int pid,int myid){
		this.TotalLock.lock();
		//System.out.println("RecieveAck length set"+this.RecievedAck.length+" pid: "+pid);
		if(pid!=myid)
		if(this.RecievedAck[pid]==0) {this.RecievedAck[pid]=1;this.NumberofAcks++;}
		//System.out.println("update completed RecievedAck");
		if(this.NumberofAcks == this.NumberofProcess-1)
			this.Lockheld();
		this.TotalLock.unlock();
	}
	public void insertWaiter(int id)
	{
		this.TotalLock.lock();
		this.WaitingList.add(id); 
		this.TotalLock.unlock();
	}
	public int write(int id,int LIMIT)
	{
		this.RecvAcknowledge(id, id);
		this.TotalLock.lock();
		while(this.State!=2)
			this.Ready.awaitUninterruptibly();
		this.TotalLock.unlock();
		int val= 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(this.fileName));
			String lastLine = "",CurrentLine="";
			while ((CurrentLine = br.readLine()) != null) {
		       // System.out.println(sCurrentLine);
		        lastLine = CurrentLine;
		    }
			//System.out.println(lastLine);
			val = Integer.parseInt(lastLine.split(":")[0]);
			//System.out.println(lastLine+" "+ val);
		} catch (FileNotFoundException e) {
			System.out.println("write: "+this.fileName +" doesn't exist");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("write: "+this.fileName +" read error");
			e.printStackTrace();
		}
		if(val==LIMIT) return LIMIT;
		val =val+1;
		try
		{
		    FileWriter fw = new FileWriter(this.fileName,true);//the true will append the new data
		    this.TotalLock.lock();
		    fw.write(val+":"+id+":"+this.RequestClock.toString()+'\n');//appends the string to the file
		    this.TotalLock.unlock();
		    fw.close();
		}
		catch(IOException ioe)
		{
		    System.err.println("IOException write error: " + ioe.getMessage());
		}
		return val;
	}
}
