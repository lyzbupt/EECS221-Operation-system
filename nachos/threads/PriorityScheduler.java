package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param transferPriority <tt>true</tt> if this queue should transfer
	 * priority from waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			ret = false;
		else
			setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			ret = false;
		else
			setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;

	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;

	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param thread the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			if (this.holder != null && this.transferPriority){
				this.holder.resources.remove(this);
			}
			getThreadState(thread).acquire(this);
			this.holder = getThreadState(thread);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			if (this.holder != null && this.transferPriority)
				this.holder.resources.remove(this);
			if (!this.waitQueue.isEmpty()){
				ThreadState firstThread = this.pickNextThread();
				if (firstThread != null){
					this.waitQueue.remove(firstThread);
					firstThread.acquire(this);
				}
				return firstThread.thread;
			}
			return null;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			// implement me
			ThreadState next = null;
			for (ThreadState ts:waitQueue) {
				if (next == null || ts.getEffectivePriority()>next.getEffectivePriority())
					next = ts;
			}
			return next;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}

		public void setDirty(){
			if (!transferPriority)
				return;
			dirty = true;
			if (this.holder != null) {
				holder.setDirty();
			}
		}

		public int getEffectivePriority() {
			effective = priorityMinimum;
			if (transferPriority) {
				if (dirty) {
					for (ThreadState t:waitQueue){
						effective = Math.max(effective,t.getEffectivePriority());
					}
					dirty = false;
				}
			}
			return effective;
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;

		public ThreadState holder = null;
		public ArrayList<ThreadState> waitQueue = new ArrayList<ThreadState>();
		public boolean dirty;
		public int effective;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 *
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param thread the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;

			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// implement me
			//return priority;
			int effective = this.priority;
			if (dirty) {
				for (ThreadQueue tq:resources){
					PriorityQueue pq = (PriorityQueue)tq;
					effective = Math.max(effective,pq.getEffectivePriority());
				}
			}
			return effective;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param priority the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;

			// implement me
			setDirty();
		}

		public void setDirty() {
			if (this.dirty)
				return;
			this.dirty = true;
			PriorityQueue pq = (PriorityQueue)waiting;
			if (pq != null)
				pq.setDirty();
		}


		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 *
		 * @param waitQueue the queue that the associated thread is now waiting
		 * on.
		 *
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			waitQueue.waitQueue.add(getThreadState(thread));

			waitQueue.setDirty();
			waiting = waitQueue;
			if (resources.contains(waitQueue)){
				resources.remove(waitQueue);
				waitQueue.holder = null;
			}
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			// implement me
			resources.add(waitQueue);
			if (waiting == waitQueue)
				waiting = null;
			setDirty();
		}

		/** The thread with which this object is associated. */
		protected KThread thread;

		/** The priority of the associated thread. */
		protected int priority;

		protected LinkedList<ThreadQueue> resources = new LinkedList<ThreadQueue>();
		protected ThreadQueue waiting;
		protected int effective;
		protected boolean dirty;
	}

	private static class FirstStartTest implements Runnable {
		String name ;
		Lock lock;

		FirstStartTest(String name, Lock lock) {
			this.name = name;
			this.lock = lock;
		}
		public void run() {
			lock.acquire();

			PriorityLockTest testThread2 = new PriorityLockTest("testThread 2", lock);
			PriorityTest testThread3 = new PriorityTest("testThread 3");

			KThread two = new KThread(testThread2).setName("testThread 2");
			KThread three = new KThread(testThread3).setName("testThread 3");

			Machine.interrupt().disable();
			ThreadedKernel.scheduler.setPriority(two, 5);
			ThreadedKernel.scheduler.setPriority(three, 5);
			Machine.interrupt().enable();

			two.fork();
			three.fork();

			for(int i = 0;i < 3;i++) {
				System.out.println(name + " looped " + i + " times");
				KThread.yield();
			}

			lock.release();
		}
	}

	private static class PriorityLockTest implements Runnable {
		String name ;
		Lock lock;

		PriorityLockTest(String name, Lock lock) {
			this.name = name;
			this.lock = lock;
		}

		public void run() {
			lock.acquire();

			for(int i = 0;i < 3;i++) {
				System.out.println(name + " looped " + i + " times");
				KThread.yield();
			}

			lock.release();
		}
	}

	private static class PriorityTest implements Runnable {
		String name ;

		PriorityTest(String name) {
			this.name = name;
		}

		public void run() {
			for(int i = 0;i < 3;i++) {
				System.out.println(name + " looped " + i + " times");
				KThread.yield();
			}
		}
	}
	public static void selfTest() {
		Lock mutex = new Lock();

		PriorityTest testThread1 = new PriorityTest("testThread 1");

		KThread one = new KThread(testThread1).setName("testThread 1");

		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(one, 3);
		Machine.interrupt().enable();

		one.fork();

		for(int i = 0;i < 10;i++) {
			KThread.currentThread().yield();
		}
	}
}
