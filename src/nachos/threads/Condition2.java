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

	conditionLock.release();//�ͷ���
boolean preState = Machine.interrupt().disable();//���ж�
waitQueue.add(KThread.currentThread());//����ǰ�̼߳��뵽waitQueue��
KThread.currentThread().sleep();//�õ�ǰ�߳�˯��
Machine.interrupt().restore(preState);//�ָ��ж�
	conditionLock.acquire();//������
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
boolean preState = Machine.interrupt().disable();//���ж�
if(!waitQueue.isEmpty()){//����waitQueue�е�һ���߳�
	KThread a = waitQueue.removeFirst();//ȡ��waitForQueue��һ���߳�
	a.ready();//��ȡ�����߳�����
}
Machine.interrupt().restore(preState);//�ָ��ж�
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

while(waitQueue!=null){//��waitQueue�е������߳̾�����
	wake();
}

    }

private LinkedList<KThread> waitQueue;
    private Lock conditionLock;
}
