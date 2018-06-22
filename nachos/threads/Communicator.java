package nachos.threads;

import java.util.ArrayList;
import java.util.Collections;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		this.lock = new Lock();
		this.speaker = new Condition(lock);
		this.listener = new Condition(lock);
		this.isReady = false;
        this.currentspeaker = new Condition(lock);//NEW
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
		lock.acquire();
		countspeaker = countspeaker + 1;
        
		while(countlistener == 0 || isReady) {
			speaker.sleep();
		}
		
		this.word = word;
		isReady = true;
		listener.wake();//Just to be safe.
		countspeaker = countspeaker - 1;
        currentspeaker.sleep(); //NEW
		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		lock.acquire();
		countlistener = countlistener + 1;
       
		while(isReady == false) {
			speaker.wake();//Just to be safe.
			listener.sleep();
		}
        
		int w;
		w = this.word;
		isReady = false;
		countlistener  = countlistener - 1;
        currentspeaker.wake(); //NEW
        speaker.wake(); //NEW
		lock.release();
		
		return w;
	}
	private int countspeaker = 0;   // The number of speaks
	private int countlistener = 0;  // The number of listen
    private boolean isReady;        // If the message is ready
	private int word;               // The message
	private Lock lock;
	private Condition speaker;      // speak will wait on this CV
	private Condition listener;     // listen will wait on this CV
    private Condition currentspeaker;
}


