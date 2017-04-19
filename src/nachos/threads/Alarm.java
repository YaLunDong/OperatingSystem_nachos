package nachos.threads;

import java.util.LinkedList;

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
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {

boolean preState = Machine.interrupt().disable();//���ж�
WaitForAlarmThread x;
for(java.util.Iterator i = waitForAlarmThreadList.iterator();i.hasNext();){
	x = (WaitForAlarmThread)i.next();//ȡ�������е�ÿ���߳��ж��Ƿ�ﵽ����ʱ��
	if(x.wakeTime<=Machine.timer().getTime()){//����ﵽ����ʱ�䣬������������Ƴ������Ѹ��߳�
		i.remove();
		x.thread.ready();
	}
}
Machine.interrupt().restore(preState);//�ָ��ж�
	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	
    //long wakeTime = Machine.timer().getTime() + x;
	//while (wakeTime > Machine.timer().getTime())
    //KThread.yield();
boolean preState = Machine.interrupt().disable();//���ж�
long wakeTime = Machine.timer().getTime()+x;//���㻽�ѵ�ʱ��
WaitForAlarmThread waitForAlarmThread = new WaitForAlarmThread(wakeTime, KThread.currentThread());
waitForAlarmThreadList.add(waitForAlarmThread);//���̼߳��뵽�ȴ�������
KThread.sleep();//�ø��߳�˯��
Machine.interrupt().restore(preState);//�ָ��ж�   
    }
    
class WaitForAlarmThread{
	long wakeTime;
	KThread thread;
	public WaitForAlarmThread(long wakeTime,KThread thread){
		this.wakeTime=wakeTime;
		this.thread=thread;
	}
}

private static LinkedList<WaitForAlarmThread> waitForAlarmThreadList = new LinkedList<WaitForAlarmThread>(); 
public static void AlarmTest(){
	KThread a = new KThread(new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			System.out.println("�߳�1����");
			for(int i = 0;i<5;i++){
				if(i == 2){
					System.out.println("�߳�1Ҫ��ʱ���ˣ���ʱʱ��Ϊ��"+Machine.timer().getTime()+",��Լ800clock ticks֮���ټ�");
					new Alarm().waitUntil(800);
					System.out.println("�߳�1�����ˣ���ʱʱ��Ϊ��"+Machine.timer().getTime());
				}
				System.out.println("*** thread 1 looped "
						   + i + " times");
				KThread.currentThread().yield();
			}
		}
	});
	a.fork();
	for(int i = 0;i<5;i++){
			if(i == 2){
				System.out.println("�߳�0Ҫ��ʱ���ˣ���ʱʱ��Ϊ��"+Machine.timer().getTime()+",��Լ1700clock ticks֮���ټ�");
				new Alarm().waitUntil(1700);
				System.out.println("�߳�0�����ˣ���ʱʱ��Ϊ��"+Machine.timer().getTime());
			}
		System.out.println("*** thread 0 looped "
				   + i + " times");
		KThread.currentThread().yield();
	}
}
}
