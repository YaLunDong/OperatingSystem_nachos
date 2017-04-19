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
ThreadState x = pickNextThread();//��һ��ѡ����߳�
if(x == null)//���Ϊnull,�򷵻�null
	return null;
KThread thread = x.thread;
getThreadState(thread).acquire(this);//���õ����̸߳�Ϊthis�̶߳��еĶ���ͷ
//System.out.println(thread.getName());
return thread;//�����̷߳���
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
java.util.Iterator i = waitList.iterator();//�߳�����ĵ�����
KThread nextthread;
if(i.hasNext()){
nextthread = (KThread)i.next();//ȡ����һ���߳�
//System.out.println(nextthread.getName());
KThread x = null;
while(i.hasNext()){//�Ƚ��̵߳���Ч���ȼ���ѡ�����ģ�������ȼ���ͬ����ѡ��ȴ�ʱ�����
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
protected KThread lockHolder = null;//����ͷ
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
	effectivePriority = priority;//�Ƚ��Լ������ȼ�������Ч���ȼ�
	for(Iterator i = acquired.iterator();i.hasNext();){//�Ƚ�acquired�е����еȴ������е������̵߳����ȼ�
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
	if(effectivePriority==-2){//==-2֤�������ȼ��̶߳��в��������ȼ�����
		//System.out.println("û�з��������ȼ���ת"+this.thread.getName()+":"+this.priority);
		return priority;
	}
	 //System.out.println("���ȼ���ת������δִ��"+this.thread.getName()+":"+this.priority+":"+this.effectivePriority);
	 return effectivePriority;//������߳�û��ִ�У���ô��֮ǰ�����Ч���ȼ�������������һ��
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
waitQueue.waitList.add(this.thread);//�������̼߳��뵽�ȴ�����
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
waitQueue.waitList.remove(this.thread);//�����������д��ڸ��̣߳�ɾ��
//if(waitQueue.waitList.isEmpty()) acquired.remove(waitQueue);
waitQueue.lockHolder = this.thread;//����readyQueue������lockHolderΪִ���̣߳�����Lock���waitQueue������lockHolderΪ�����ߣ�����waitForJoin����������lockHolderΪִ��join�������̡߳�
if(waitQueue.transferPriority){//����������ȼ���ת����ִ���������
this.effectivePriority = invalidPriority;
acquired.add(waitQueue);//���ȴ����̵߳Ķ��м�����̵߳ĵȴ����м��ϼ�����
}
//if(waitQueue!=KThread.getReadyQueue()&&waitQueue.transferPriority){
//acquired.add(waitQueue);//���ȴ����̵߳Ķ��м�����̵߳ĵȴ����м��ϼ�����
////this.effectivePriority = invalidPriority;
//
//}
	}	

	/** The thread with which this object is associated. */
protected int effectivePriority = -2;//��Ч���ȼ���ʼ��Ϊ-2
protected final int invalidPriority = -1;
//private long effectWaitForAccessTime = -2;
//private long invalidWaitForAccessTim = -1;
//protected PriorityQueue Twait = null;
protected HashSet<nachos.threads.PriorityScheduler.PriorityQueue> acquired = new HashSet<nachos.threads.PriorityScheduler.PriorityQueue>();//�ȴ����̵߳��������ȶ��У�ÿ�����ȶ������еȴ��̣߳�,�����ȴ������ȴ�join����

	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
    }
    

//���� 
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
	boolean status = Machine.interrupt().disable();//���жϣ�setPriority()������Ҫ����ж�
    final KThread a = new KThread(new PingTest(1)).setName("thread1");
	new PriorityScheduler().setPriority(a,2);
	System.out.println("thread1�����ȼ�Ϊ��"+new PriorityScheduler().getThreadState(a).priority);
	KThread b = new KThread(new PingTest(2)).setName("thread2");
	new PriorityScheduler().setPriority(b,4);
	System.out.println("thread2�����ȼ�Ϊ��"+new PriorityScheduler().getThreadState(b).priority);
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
	System.out.println("thread3�����ȼ�Ϊ��"+new PriorityScheduler().getThreadState(c).priority);
	a.fork();
	b.fork();
	c.fork();
	//KThread.currentThread().ready();
	Machine.interrupt().restore(status);
}


}
