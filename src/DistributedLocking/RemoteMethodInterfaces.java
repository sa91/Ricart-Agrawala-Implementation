package DistributedLocking;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.google.protobuf.InvalidProtocolBufferException;
public interface RemoteMethodInterfaces extends Remote {
	public byte[] LockRequest(byte msg[]) throws RemoteException;
	public byte[] UnlockRequest(byte msg[]) throws RemoteException;
	//public byte[] LockRequest(byte msg[]) throws RemoteException;
}

