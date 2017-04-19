package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
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
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
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
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
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
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
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
	    getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
ThreadState x = pickNextThread();//下一个选择的线程
if(x == null)//如果为null,则返回null
	return null;
KThread thread = x.thread;
getThreadState(thread).acquire(this);//将得到的线程改为this线程队列的队列头
//System.out.println(thread.getName());
return thread;//将该线程返回
	    // implement me
	    //return null;
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
java.util.Iterator i = waitList.iterator();//线程链表的迭代器
KThread nextthread;
if(i.hasNext()){
nextthread = (KThread)i.next();//取出下一个线程
//System.out.println(nextthread.getName());
KThread x = null;
while(i.hasNext()){//比较线程的有效优先级，选出最大的，如果优先级相同，则选择等待时间最长的
	x = (KThread)i.next();
	//System.out.println(x.getName());
	int a = getThreadState(nextthread).getEffectivePriority();
	int b = getThreadState(x).getEffectivePriority();
	if(a<b){
//		if(a==b){
//			//if(getThreadState(nextthread).effectWaitForAccessTime>getThreadState(x).effectWaitForAccessTime)
//				nextthread = x;
//		}else{
			nextthread = x;		
//	}
	}
}
return getThreadState(nextthread);
}else 
return null;
		// implement me
	    //return null;
	}
	
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
protected KThread lockHolder = null;//队列头
protected LinkedList<KThread> waitList = new LinkedList<KThread>();
	public boolean transferPriority;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;
	    
	    setPriority(priorityDefault);

	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */

	public int getEffectivePriority() {
Lib.assertTrue(Machine.interrupt().disabled());
if(effectivePriority == invalidPriority&&!acquired.isEmpty()){
	effectivePriority = priority;//先将自己的优先级赋给有效优先级
	for(Iterator i = acquired.iterator();i.hasNext();){//比较acquired中的所有等待队列中的所有线程的优先级
		for(Iterator j = ((PriorityQueue)i.next()).waitList.iterator();j.hasNext();){
			ThreadState ts = getThreadState((KThread)j.next());
			if(ts.priority>effectivePriority){
				effectivePriority = ts.priority;
				//effectWaitForAccessTime = ts.thread.waitForAccessTime;
			}
		}
	}
	//System.out.println("hh");
	return effectivePriority;
}else{ 
	//effectWaitForAccessTime=this.thread.waitForAccessTime;
	if(effectivePriority==-2){//==-2证明该优先级线程队列不存在优先级倒置
		//System.out.println("没有发生过优先级翻转"+this.thread.getName()+":"+this.priority);
		return priority;
	}
	 //System.out.println("优先级反转过，且未执行"+this.thread.getName()+":"+this.priority+":"+this.effectivePriority);
	 return effectivePriority;//如果该线程没有执行，那么它之前算的有效优先级不必重新再算一遍
	//return priority;
}
		// implement me
	   
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;
	    
	    this.priority = priority;
	    // implement me
	}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
waitQueue.waitList.add(this.thread);//将调用线程加入到等待队列
// implement me
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
waitQueue.waitList.remove(this.thread);//如果这个队列中存在该线程，删除
//if(waitQueue.waitList.isEmpty()) acquired.remove(waitQueue);
waitQueue.lockHolder = this.thread;//对于readyQueue来讲，lockHolder为执行线程；对于Lock类的waitQueue来讲，lockHolder为持锁者；对于waitForJoin队列来讲，lockHolder为执行join方法的线程。
if(waitQueue.transferPriority){//如果存在优先级翻转，则执行下面操作
this.effectivePriority = invalidPriority;
acquired.add(waitQueue);//将等待该线程的队列加入该线程的等待队列集合集合中
}
//if(waitQueue!=KThread.getReadyQueue()&&waitQueue.transferPriority){
//acquired.add(waitQueue);//将等待该线程的队列加入该线程的等待队列集合集合中
////this.effectivePriority = invalidPriority;
//
//}
	}	

	/** The thread with which this object is associated. */
protected int effectivePriority = -2;//有效优先级初始化为-2
protected final int invalidPriority = -1;
//private long effectWaitForAccessTime = -2;
//private long invalidWaitForAccessTim = -1;
//protected PriorityQueue Twait = null;
protected HashSet<nachos.threads.PriorityScheduler.PriorityQueue> acquired = new HashSet<nachos.threads.PriorityScheduler.PriorityQueue>();//等待该线程的所有优先队列（每个优先队列里有等待线程）,包括等待锁，等待join方法

	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
    }
    

//测试 
private static class PingTest implements Runnable {
	PingTest(int which) {
	    this.which = which;
	}
	
	public void run() {
	    for (int i=0; i<5; i++) {
		System.out.println("*** thread " + which + " looped "
				   + i + " times");
		KThread.currentThread().yield();
	    }
	}

	private int which;
    }
public static void PriorityTest(){
	boolean status = Machine.interrupt().disable();//关中断，setPriority()函数中要求关中断
    final KThread a = new KThread(new PingTest(1)).setName("thread1");
	new PriorityScheduler().setPriority(a,2);
	System.out.println("thread1的优先级为："+new PriorityScheduler().getThreadState(a).priority);
	KThread b = new KThread(new PingTest(2)).setName("thread2");
	new PriorityScheduler().setPriority(b,4);
	System.out.println("thread2的优先级为："+new PriorityScheduler().getThreadState(b).priority);
	KThread c = new KThread(new Runnable(){
		public void run(){
			for (int i=0; i<5; i++) {
				if(i==2) 
					a.join();
				System.out.println("*** thread 3 looped "
						   + i + " times");
				KThread.currentThread().yield();
			}
		}
	}).setName("thread3");
	new PriorityScheduler().setPriority(c,6);
	System.out.println("thread3的优先级为："+new PriorityScheduler().getThreadState(c).priority);
	a.fork();
	b.fork();
	c.fork();
	//KThread.currentThread().ready();
	Machine.interrupt().restore(status);
}


}
