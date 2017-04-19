package nachos.threads;

import nachos.machine.*;
import java.util.Random;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	// implement me
	return new LotteryQueue(transferPriority);
    }
    
protected LottoryThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new LottoryThreadState(thread);

	return (LottoryThreadState) thread.schedulingState;
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

private static final int priorityMinimum = 0;
private static final int priorityMaximum = Integer.MAX_VALUE;

class LotteryQueue extends PriorityQueue{

	LotteryQueue(boolean transferPriority) {
		super(transferPriority);
		// TODO Auto-generated constructor stub
	}
	
	protected LottoryThreadState pickNextThread() {
		java.util.Iterator i = waitList.iterator();
		int sumTickets = 0;
		KThread x;
		KThread nextThread = null;
		while(i.hasNext()){
			x = (KThread)i.next();
			sumTickets +=getThreadState(x).getEffectivePriority();
//if(this==KThread.getReadyQueue())
//	System.out.print(x.getName()+"有效优先级:"+getThreadState(x).getEffectivePriority()+",");
		}
		int winningTickets=0;
		if(sumTickets>0)
			winningTickets = new Random().nextInt(sumTickets);
//if(this==KThread.getReadyQueue()){
//System.out.print("总票数为"+sumTickets+",");
//System.out.println("中奖彩票为"+winningTickets);}
		i = waitList.iterator();
		int countTickets = 0;
		
		while(i.hasNext()){
			
			nextThread = (KThread)i.next();
//System.out.println(nextThread.getName());
			countTickets += getThreadState(nextThread).getEffectivePriority();
//System.out.println("countTickets:"+countTickets);
			if(countTickets>=winningTickets)
				break;
		}
		
		if(nextThread == null) return null;
		else return getThreadState(nextThread);
			}	
	
	
}

class LottoryThreadState extends ThreadState{

	public LottoryThreadState(KThread thread) {
		super(thread);
		// TODO Auto-generated constructor stub
	}
	
	public int getEffectivePriority() {
		Lib.assertTrue(Machine.interrupt().disabled());
		if(effectivePriority == invalidPriority&&!acquired.isEmpty()){
			effectivePriority = priority;//先将自己的优先级赋给有效优先级
			for(Iterator i = acquired.iterator();i.hasNext();){//比较acquired中的所有等待队列中的所有线程的优先级
				for(Iterator j = ((PriorityQueue)i.next()).waitList.iterator();j.hasNext();){
					ThreadState ts = getThreadState((KThread)j.next());
						effectivePriority += ts.priority;
				}
			}
			return effectivePriority;
		}else{ 
			if(effectivePriority==-2){//==-2证明该优先级线程队列不存在优先级倒置
				return priority;
			}
		
		 return effectivePriority;//如果该线程没有执行，那么它之前算的有效优先级不必重新再算一遍
		}
			   
			}
}

//测试 
private static class PingTest implements Runnable {
	PingTest(int which) {
	    this.which = which;
	}
	
	public void run() {
	    for (int i=0; i<3; i++) {
		System.out.println("*** thread " + which + " looped "
				   + i + " times");
		KThread.currentThread().yield();
	    }
	    System.out.println("thread 1 将执行结束");
	}

	private int which;
  }
public static void LotteryTest(){
	boolean status = Machine.interrupt().disable();//关中断，setPriority()函数中要求关中断
    final KThread a = new KThread(new PingTest(1)).setName("thread1");
	new LotteryScheduler().setPriority(a,30);
	System.out.println("thread1的优先级为："+new LotteryScheduler().getThreadState(a).priority);
	
	KThread b = new KThread(new Runnable(){
		public void run(){
			for (int i=0; i<3; i++) {
				if(i==1){ 
					System.out.println("将要发生优先级捐赠！");
					a.join();
				}
				System.out.println("*** thread 2 looped "
						   + i + " times");
				
				KThread.currentThread().yield();
			}
			System.out.println("thread 2将执行完毕");
		}
	}).setName("thread2");
	new LotteryScheduler().setPriority(b,90);
	System.out.println("thread2的优先级为："+new LotteryScheduler().getThreadState(b).priority);
	System.out.println("thread main的优先级为："+new LotteryScheduler().getThreadState(KThread.currentThread()).priority);
	a.fork();
	b.fork();
	for (int i=0; i<2; i++) {
		System.out.println("*** thread main looped "
				   + i + " times");
		KThread.currentThread().yield();
	}
	//KThread.currentThread().ready();
	Machine.interrupt().restore(status);
	Machine.halt();
}

}
