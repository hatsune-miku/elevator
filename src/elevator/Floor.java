/*
 * Created by Zhen Guan
 * Student number: 202191382
 * Email: zguan@mun.ca
 */

package elevator;

import java.util.concurrent.locks.ReentrantLock;

public class Floor {
    boolean isUpRequested;
    boolean isDownRequested;

    private ReentrantLock upLock;
    private ReentrantLock downLock;

    public Floor() {
        this.upLock = new ReentrantLock();
        this.downLock = new ReentrantLock();
        this.isDownRequested = false;
        this.isUpRequested = false;
    }

    public boolean tryLockUp() {
        return upLock.tryLock();
    }

    public boolean tryLockDown() {
        return downLock.tryLock();
    }

    public void unlockUp() {
        upLock.unlock();
    }

    public void unlockDown() {
        downLock.unlock();
    }

    public boolean isUpRequested() {
        return isUpRequested;
    }

    public void setUpRequested(boolean upRequested) {
        isUpRequested = upRequested;
    }

    public boolean isDownRequested() {
        return isDownRequested;
    }

    public void setDownRequested(boolean downRequested) {
        isDownRequested = downRequested;
    }
}
