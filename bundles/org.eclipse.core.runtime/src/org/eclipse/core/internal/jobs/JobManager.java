/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import java.util.*;

import org.eclipse.core.internal.locks.Queue;
import org.eclipse.core.internal.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.*;

public class JobManager implements IJobManager {
	private static JobManager instance;
	private final ProgressHandler progressHandler = new ProgressHandler(this);
	private final List listeners = Collections.synchronizedList(new ArrayList());
	
	/**
	 * The lock for synchronizing all activity in the job manager.  To avoid deadlock,
	 * this lock must never be held for extended periods, and must never be
	 * held while third party code is being called.
	 */
	private final Object lock = new Object();
	
	/**
	 * jobs that are waiting to be run
	 */
	private final Queue waiting = new Queue();
	/**
	 * Set of all jobs.
	 */
	private final Set allJobs = new HashSet();
	/**
	 * Paused jobs that have arrived at the front of the queue.
	 */
	private final HashSet paused = new HashSet();
	/**
	 * The registered worker threads.
	 */
	private final ArrayList workers = new ArrayList();
	
	private boolean running = false;

	public static JobManager getInstance() {
		if (instance == null) {
			//ensure we don't start two job managers
			synchronized (JobManager.class) {
				if (instance == null) {
					instance = new JobManager();
					instance.startup(4);
				}
			}
		}
		return instance;
	}
	/**
	 * Shuts down the running job manager, if any.
	 */
	public static void shutdownIfRunning() {
		if (instance != null)
			instance.doShutdown();
	}
	private JobManager() {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#addListener(org.eclipse.core.runtime.jobs.IJobListener)
	 */
	public void addJobListener(IJobListener listener) {
		listeners.add(listener);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#addProgressListener(org.eclipse.core.runtime.jobs.IProgressListener)
	 */
	public void addProgressListener(IProgressListener listener) {
		progressHandler.addListener(listener);
	}
	/**
	 * Cancels a job
	 */
	boolean cancel(Job job) {
		int oldState;
		boolean wasWaiting;
		synchronized (lock) {
			allJobs.remove(job);
			wasWaiting = waiting.contains(job);
			oldState = job.getState();
			((InternalJob)job).setState(Job.NONE);
		}
		//only notify listeners if the job was waiting
		//(if the job is already running, then we notify when it finishes its run method)
		if (wasWaiting) {
			IJobListener[] listeners = getJobListeners();
			for (int i = 0; i < listeners.length; i++) {
				listeners[i].finished(job, Job.CANCELED);
			}
		}
		//cancelation may only fail if the job is currently running
		return oldState == Job.RUNNING;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#cancel(java.lang.String)
	 */
	public void cancel(String family) {
	}
	/**
	 * Returns the next job to be run.  If no jobs are waiting to run,
	 * this method will block until a job is available.  The worker must
	 * call endJob when the job is finished running.
	 * @return
	 */
	void endJob(Job job, int result) {
		synchronized (lock) {
			//if the job was not running then it is a worker programming error
			if (!allJobs.remove(job) || job.getState() != Job.RUNNING) {
				Assert.isLegal(false, "Worker ended a job it didn't start");
				return;
			}	
			((InternalJob)job).setState(Job.NONE);
		}
		//notify listeners outside sync block
		IJobListener[] listeners = getJobListeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].finished(job, result);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#find(java.lang.String)
	 */
	public Job[] find(String family) {
		return null;
	}
	private IJobListener[] getJobListeners() {
		synchronized (lock) {
			return (IJobListener[]) listeners.toArray(new IJobListener[listeners.size()]);
		}
	}
	IProgressMonitor getProgressHandler() {
		return progressHandler;
	}
	/**
	 * Request to pause the given job. Return true if the job was successfully paused.
	 * @param job
	 */
	boolean pause(InternalJob job) {
		synchronized (lock) {
			//cannot be paused if it is already running
			if (job.getState() == Job.RUNNING)
				return false;
			job.setState(Job.PAUSED);
			return true;
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#pause(java.lang.String)
	 */
	public void pause(String family) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#removeListener(org.eclipse.core.runtime.jobs.IJobListener)
	 */
	public void removeJobListener(IJobListener listener) {
		listeners.remove(listener);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#removeProgressListener(org.eclipse.core.runtime.jobs.IProgressListener)
	 */
	public void removeProgressListener(IProgressListener listener) {
		progressHandler.removeListener(listener);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#resume(java.lang.String)
	 */
	public void resume(String family) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#schedule(ob)
	 */
	public void schedule(Job job) {
		schedule(job, 0);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#schedule(Job, JobFamily)
	 */
	public void schedule(Job job, IJobFamily family) {
		((InternalJob)job).setFamily(family);
		schedule(job);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#schedule(Job, long)
	 */
	public void schedule(Job job, long delay) {
		Assert.isNotNull(job, "Job is null");
		//notify listeners outside sync block
		IJobListener[] listeners = getJobListeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].aboutToSchedule(job);
		}
		synchronized (lock) {
			((InternalJob)job).setState(Job.WAITING);
			waiting.enqueue(job);
			allJobs.add(job);
			//wake up a worker thread that is waiting on the lock
			lock.notify();
		}
	}
	/**
	 * Shuts down the job manager.  Currently running jobs will be told
	 * to stop, but worker threads may still continue processing.
	 */
	private void doShutdown() {
		synchronized (lock) {
			running = false;
			//clean up
			workers.clear();
			paused.clear();
			waiting.clear();
			//discard all jobs (progress callbacks from running jobs
			//will now think the jobs are canceled, and should terminate
			//in a timely fashion)
			allJobs.clear();
			//wake up all workers so they know it's time to go home
			lock.notifyAll();
		}
	}
	/**
	 * Returns the next job to be run.  If no jobs are waiting to run,
	 * this method will block until a job is available.  The worker must
	 * call endJob when the job is finished running.  This method returns null
	 * only if the job manager has been shut down.
	 * @return
	 */
	Job startJob() {
		while (true) {
			Job job = nextJob();
			if (job == null)
				return null;
			//must perform this outside sync block because it is third party code
			if (job.shouldRun()) {
				((InternalJob)job).setState(Job.RUNNING);
			} else {
				((InternalJob)job).setState(Job.NONE);
				continue;
			}
			//todo check listeners for veto
			return job;
		}
	}
	/**
	 * Removes and returns the first waiting job in the queue. If the queue is empty,
	 * this method blocks until a job becomes available.  Returns null only if
	 * the job manager has been shutdown.
	 */
	private Job nextJob() {
		synchronized (lock) {
			Job job = (Job)waiting.dequeue();
			while (true) {
				while (job == null) {
					//wait until a job is added to the queue
					try {
						lock.wait();
					} catch (InterruptedException e) {
					}
					//we may have been woken up because of a shutdown
					if (!running)
						return null;
					job = (Job)waiting.dequeue();
				}
				if (job.getState() == Job.WAITING) {
					return job;
				}
				if (job.getState() == Job.PAUSED) {
					//if job is paused, add it to the list of paused jobs
					paused.add(job);
				}
				//otherwise job is canceled, so just discard it
			}
		}
	}
	/**
	 * Starts the job manager, with the given number of worker threads.
	 */
	private void startup(int workerCount) {
		synchronized (lock) {
			running = true;
			Assert.isLegal(workerCount > 0, "A manager must have workers");
			for (int i = 0; i < workerCount; i++) {
				Worker worker = new Worker(this);
				workers.add(worker);
				worker.start();
			}
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#wait(org.eclipse.core.runtime.jobs.Job, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void wait(Job job, IProgressMonitor monitor) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#wait(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void wait(String family, IProgressMonitor monitor) {
	}
}