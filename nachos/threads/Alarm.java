package nachos.threads;

import java.util.*;
import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	private static LinkedList<alarmthread> waitforwake = new LinkedList<alarmthread>();
	
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		
		boolean intStatus = Machine.interrupt().disable();
		long time = Machine.timer().getTime();
		if (waitforwake.isEmpty() == true) {
		}
		else {
			for(int i = 0; i < waitforwake.size(); i++) {
				alarmthread temp = waitforwake.get(i);
				if(time >= temp.wakeuptime) {
					waitforwake.remove(i);
					temp.thread.ready();
				}
			}
			
		}
		
		KThread.currentThread().yield();
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		
		long wakeTime = Machine.timer().getTime() + x;
		boolean intStatus = Machine.interrupt().disable();
		
		alarmthread athread = new alarmthread(wakeTime, KThread.currentThread());
		waitforwake.add(athread);
		KThread.sleep();
		
		Machine.interrupt().restore(intStatus);
		//while (wakeTime > Machine.timer().getTime())
			//KThread.yield();
	}
	
	private class alarmthread {
		/*save the current thread and waketime*/
		alarmthread(long wakeuptime, KThread thread){
			this.wakeuptime = wakeuptime;
			this.thread = thread;
		}
		public long wakeuptime;
		KThread thread;
	}
}
