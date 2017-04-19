package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
waitQueue = new LinkedList<KThread>();
	this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

	conditionLock.release();//释放锁
boolean preState = Machine.interrupt().disable();//关中断
waitQueue.add(KThread.currentThread());//将当前线程加入到waitQueue中
KThread.currentThread().sleep();//让当前线程睡眠
Machine.interrupt().restore(preState);//恢复中断
	conditionLock.acquire();//申请锁
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
boolean preState = Machine.interrupt().disable();//关中断
if(!waitQueue.isEmpty()){//唤醒waitQueue中的一个线程
	KThread a = waitQueue.removeFirst();//取出waitForQueue中一个线程
	a.ready();//将取出的线程启动
}
Machine.interrupt().restore(preState);//恢复中断
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

while(waitQueue!=null){//将waitQueue中的所有线程均唤醒
	wake();
}

    }

private LinkedList<KThread> waitQueue;
    private Lock conditionLock;
}
