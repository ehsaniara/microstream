package net.jadoth.storage.types;

import static net.jadoth.X.notNull;

import java.io.File;
import java.util.function.Predicate;

import net.jadoth.collections.types.XGettingEnum;
import net.jadoth.memory.Chunks;
import net.jadoth.swizzling.types.SwizzleIdSet;
import net.jadoth.util.UtilStackTrace;

public interface StorageTaskBroker
{
	public StorageTask currentTask();

	public StorageRequestTaskLoadRoots enqueueRootsLoadTask()
		throws InterruptedException;

	public StorageRequestTaskLoadByTids enqueueLoadTaskByTids(SwizzleIdSet loadTids)
		throws InterruptedException;

	public StorageRequestTaskLoadByOids enqueueLoadTaskByOids(SwizzleIdSet[] loadOids)
		throws InterruptedException;
	
	public StorageRequestTaskStoreEntities enqueueStoreTask(Chunks[] medium)
		throws InterruptedException;

	public default StorageRequestTaskExportEntitiesByType enqueueExportTypesTask(
		final StorageEntityTypeExportFileProvider exportFileProvider
	)
		throws InterruptedException
	{
		return this.enqueueExportTypesTask(exportFileProvider, null);
	}
	
	public StorageRequestTaskExportEntitiesByType enqueueExportTypesTask(
		StorageEntityTypeExportFileProvider            exportFileProvider,
		Predicate<? super StorageEntityTypeHandler<?>> isExportType
	)
		throws InterruptedException;
	
	

	public StorageRequestTask enqueueExportChannelsTask(
		StorageIoHandler fileHandler             ,
		boolean          performGarbageCollection
	)
		throws InterruptedException;

	public StorageRequestTask enqueueImportFromFilesTask(XGettingEnum<File> importFiles)
		throws InterruptedException;

	public StorageRequestTaskCreateStatistics enqueueCreateRawFileStatisticsTask()
		throws InterruptedException;

	public default StorageChannelTaskInitialize issueChannelInitialization(
		final StorageChannelController    channelController               ,
		final StorageEntityCacheEvaluator entityInitializingCacheEvaluator
	)
		throws InterruptedException
	{
		// oldTypes dictionary is optional, may be null
		return this.issueChannelInitialization(channelController, entityInitializingCacheEvaluator, null);
	}

	public StorageChannelTaskInitialize issueChannelInitialization(
		StorageChannelController    channelController               ,
		StorageEntityCacheEvaluator entityInitializingCacheEvaluator,
		StorageTypeDictionary       oldTypes
	)
		throws InterruptedException;

	public StorageChannelTaskShutdown issueChannelShutdown(StorageChannelController channelController)
		throws InterruptedException;

	public StorageRequestTaskGarbageCollection issueGarbageCollection(long nanoTimeBudgetBound)
		throws InterruptedException;

	public StorageRequestTaskFileCheck issueFileCheck(
		long                               nanoTimeBudgetBound,
		StorageDataFileDissolvingEvaluator fileDissolver
	)
		throws InterruptedException;

	public StorageRequestTaskCacheCheck issueCacheCheck(
		long                        nanoTimeBudgetBound,
		StorageEntityCacheEvaluator entityEvaluator
	)
		throws InterruptedException;

	public StorageChannelTaskTruncateData issueTruncateData(StorageChannelController channelController)
		throws InterruptedException;



	public final class Implementation implements StorageTaskBroker
	{
		///////////////////////////////////////////////////////////////////////////
		// instance fields  //
		/////////////////////

		private final StorageManager            storageManager;
		private final StorageRequestTaskCreator taskCreator   ;
		private final int                       channelCount  ;

		private volatile StorageTask currentHead;



		///////////////////////////////////////////////////////////////////////////
		// constructors     //
		/////////////////////

		public Implementation(
			final StorageManager            storageManager,
			final StorageRequestTaskCreator taskCreator
		)
		{
			super();
			this.storageManager = notNull(storageManager);
			this.taskCreator    = notNull(taskCreator);
			this.channelCount   = storageManager.channelCountProvider().get();
			this.currentHead    = new StorageTask.DummyTask();
		}



		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////

		private StorageRequestTaskGarbageCollection enqueueTaskPrependingFullGc(
			final StorageTask task               ,
			final long        nanoTimeBudgetBound
		)
			throws InterruptedException
		{
			final StorageRequestTaskGarbageCollection gcTask;
			this.enqueueTasksAndNotifyAll(
				gcTask = new StorageRequestTaskGarbageCollection.Implementation(
					task.timestamp() - 1,
					this.channelCount   ,
					nanoTimeBudgetBound ,
					task
				),
				task
			);
			return gcTask;
		}

		private synchronized void enqueueTasksAndNotifyAll(
			final StorageTask firstTask ,
			final StorageTask secondTask
		)
			throws InterruptedException
		{
			/* The first task is the next task to be processed, the second task is the new head task, i.e.
			 * the new last task that gets future tasks attached to.
			 * It is the first task's responsibility to (eventually) lead to the second task in order to
			 * close the task chain.
			 */
			final StorageTask currentHead = this.enqueueTask(firstTask, secondTask);

			// notifiy waiting threads via current head
			synchronized(currentHead)
			{
//				XDebug.debugln(Thread.currentThread() + " notifying all");
				currentHead.notifyAll();
			}
		}

		private void enqueueTaskAndNotifyAll(final StorageTask task) throws InterruptedException
		{
			final StorageTask currentHead = this.enqueueTask(task);
			synchronized(currentHead)
			{
//				XDebug.debugln(Thread.currentThread() + " notifying all");
				currentHead.notifyAll();
			}
		}

		private StorageTask enqueueTask(final StorageTask task)
		{
			return this.enqueueTask(task, task);
//			final StorageTask currentHead;
//			(currentHead = this.currentHead).setNext(task);
//			this.currentHead = task;
//			return currentHead;
		}

		private StorageTask enqueueTask(final StorageTask nextTask, final StorageTask newHeadTask)
		{
			final StorageTask currentHead;
			(currentHead = this.currentHead).setNext(nextTask);
			this.currentHead = newHeadTask;
			return currentHead;
		}

		@Override
		public final StorageTask currentTask()
		{
			return this.currentHead;
		}

		@Override
		public final synchronized StorageRequestTaskGarbageCollection issueGarbageCollection(
			final long nanoTimeBudgetBound
		)
			throws InterruptedException
		{
			final StorageRequestTask dummy =
				new StorageChannelSynchronizingTask.AbstractCompletingTask.Dummy(this.channelCount)
			;
			final StorageRequestTaskGarbageCollection gcTask =
				this.enqueueTaskPrependingFullGc(dummy, nanoTimeBudgetBound)
			;
			return gcTask;
		}

		@Override
		public final synchronized StorageRequestTaskCacheCheck issueCacheCheck(
			final long                        nanoTimeBudgetBound ,
			final StorageEntityCacheEvaluator entityEvaluator
		)
			throws InterruptedException
		{
			final StorageRequestTaskCacheCheck task = this.taskCreator.createFullCacheCheckTask(
				this.channelCount,
				nanoTimeBudgetBound,
				entityEvaluator
			);
			this.enqueueTaskAndNotifyAll(task);
			return task;
		}

		@Override
		public final synchronized StorageRequestTaskFileCheck issueFileCheck(
			final long                               nanoTimeBudgetBound,
			final StorageDataFileDissolvingEvaluator fileDissolver
		)
			throws InterruptedException
		{
			final StorageRequestTaskFileCheck task = this.taskCreator.createFullFileCheckTask(
				this.channelCount,
				nanoTimeBudgetBound,
				fileDissolver
			);
			this.enqueueTaskAndNotifyAll(task);
			return task;
		}

		@Override
		public final synchronized StorageRequestTask enqueueExportChannelsTask(
			final StorageIoHandler fileHandler             ,
			final boolean          performGarbageCollection
		)
			throws InterruptedException
		{
			final StorageRequestTaskExportChannels task = this.taskCreator.createTaskExportChannels(
				this.channelCount,
				fileHandler
			);

			/*
			 * If the data shall "just" be exported as fast as possible and potential unreachable entities
			 * are not a problem, then not performing the GC is perferable.
			 * If the exported data shall represent a definite minimum of all reachable entities and the
			 * required time for a full GC is not an issue (e.g. nightly chronjob), then performing the GC
			 * is preferable.
			 * Both cases are equally viable depending on the situation. Hence the required flag.
			 */
			if(performGarbageCollection)
			{
				// enqueue task with a prepended full GC
				this.enqueueTaskPrependingFullGc(task, Long.MAX_VALUE); // must let GC complete to get viable results
			}
			else
			{
				// enqueue task directly
				this.enqueueTaskAndNotifyAll(task);
			}

			/*
			 * in both cases, the actual task is the last to be processed, so the calling thread
			 * must always wait on the actual task.
			 */

			return task;
		}

		@Override
		public StorageRequestTask enqueueImportFromFilesTask(final XGettingEnum<File> importFiles)
			throws InterruptedException
		{
			// always use the internal evaluator to match live operation
			final StorageRequestTaskImportData task = this.taskCreator.createImportFromFilesTask(
				this.channelCount                                  ,
				this.storageManager.configuration().fileEvaluator(),
				this.storageManager.objectIdRangeEvaluator()       ,
				importFiles
			);
			this.enqueueTaskAndNotifyAll(task);
			return task;
		}

		@Override
		public StorageRequestTaskCreateStatistics enqueueCreateRawFileStatisticsTask() throws InterruptedException
		{
			final StorageRequestTaskCreateStatistics task = this.taskCreator.createCreateRawFileStatisticsTask(
				this.channelCount
			);
			this.enqueueTaskAndNotifyAll(task);
			return task;
		}


		@Override
		public final synchronized StorageRequestTaskExportEntitiesByType enqueueExportTypesTask(
			final StorageEntityTypeExportFileProvider            exportFileProvider,
			final Predicate<? super StorageEntityTypeHandler<?>> isExportType
		)
			throws InterruptedException
		{
			final StorageRequestTaskExportEntitiesByType task = this.taskCreator.createExportTypesTask(
				this.channelCount ,
				exportFileProvider,
				isExportType
			);

			// must let GC complete to get viable results
			this.enqueueTaskPrependingFullGc(task, Long.MAX_VALUE);
//			DEBUGStorage.println("disabled type export prepended Gc!");
//			this.enqueueTaskAndNotifyAll(task);

			// return actual task
			return task;
		}
		
		/**
		 * The task broker cannot rely on any outside logic to pass an array with valid length or validate its length.
		 * Every channel-count-depending array must be validated right before it is enqueued as a task to prevent
		 * the system from crashing.
		 * 
		 * @param channelArray
		 */
		private void validateChannelCount(final Object[] channelArray)
		{
			if(channelArray.length == this.channelCount)
			{
				return;
			}

			// (27.04.2018 TM)EXCP: proper exception
			throw UtilStackTrace.cutStacktraceByOne(new RuntimeException());
		}

		@Override
		public final synchronized StorageRequestTaskStoreEntities enqueueStoreTask(final Chunks[] medium)
			throws InterruptedException
		{
			this.validateChannelCount(medium);
			
			// task creation must be called AFTER acquiring the lock to ensure temporal consistency in the task chain
			final StorageRequestTaskStoreEntities task = this.taskCreator.createSaveTask(medium);
			
//			((StorageRequestTaskSaveEntities.Implementation)task).DEBUG_Print(null);
			
			this.enqueueTaskAndNotifyAll(task);
			return task;
		}

		@Override
		public final synchronized StorageRequestTaskLoadByOids enqueueLoadTaskByOids(
			final SwizzleIdSet[] loadOids
		)
			throws InterruptedException
		{
			this.validateChannelCount(loadOids);
			
			// task creation must be called AFTER acquiring the lock to ensure temporal consistency in the task chain
			final StorageRequestTaskLoadByOids task = this.taskCreator.createLoadTaskByOids(loadOids);
			this.enqueueTaskAndNotifyAll(task);
			return task;
		}

		@Override
		public final synchronized StorageRequestTaskLoadRoots enqueueRootsLoadTask() throws InterruptedException
		{
			// task creation must be called AFTER acquiring the lock to ensure temporal consistency in the task chain
			final StorageRequestTaskLoadRoots task = this.taskCreator.createRootsLoadTask(this.channelCount);
			this.enqueueTaskAndNotifyAll(task);
			return task;
		}

		@Override
		public final synchronized StorageRequestTaskLoadByTids enqueueLoadTaskByTids(
			final SwizzleIdSet loadTids
		)
			throws InterruptedException
		{
			// task creation must be called AFTER acquiring the lock to ensure temporal consistency in the task chain
			final StorageRequestTaskLoadByTids task = this.taskCreator.createLoadTaskByTids(loadTids, this.channelCount);
			this.enqueueTaskAndNotifyAll(task);
			return task;
		}

		@Override
		public final synchronized StorageChannelTaskInitialize issueChannelInitialization(
			final StorageChannelController    channelController               ,
			final StorageEntityCacheEvaluator entityInitializingCacheEvaluator,
			final StorageTypeDictionary       oldTypes

		)
			throws InterruptedException
		{
			final StorageChannelTaskInitialize task = this.taskCreator.createInitializationTask(
				this.channelCount               ,
				channelController               ,
				entityInitializingCacheEvaluator,
				oldTypes
			);
			// special case: cannot wait on the task before the channel threads are started
			this.enqueueTaskAndNotifyAll(task);
			return task;
		}

		@Override
		public final synchronized StorageChannelTaskShutdown issueChannelShutdown(
			final StorageChannelController channelController
		)
			throws InterruptedException
		{
			final StorageChannelTaskShutdown task = this.taskCreator.createShutdownTask(
				this.channelCount,
				channelController
			);
			// special case: cannot wait on the task before the channel threads are started
			this.enqueueTaskAndNotifyAll(task);
			return task;
		}

		@Override
		public StorageChannelTaskTruncateData issueTruncateData(final StorageChannelController channelController)
			throws InterruptedException
		{
			final StorageChannelTaskTruncateData task = this.taskCreator.createTruncateTask(
				this.channelCount,
				channelController
			);
			// special case: cannot wait on the task before the channel threads are started
			this.enqueueTaskAndNotifyAll(task);
			return task;
		}

	}

	public interface Creator
	{
		public StorageTaskBroker createTaskBroker(
			StorageManager            storageManager,
			StorageRequestTaskCreator taskCreator
		);



		public final class Implementation implements Creator
		{
			@Override
			public StorageTaskBroker createTaskBroker(
				final StorageManager            storageManager,
				final StorageRequestTaskCreator taskCreator
			)
			{
				return new StorageTaskBroker.Implementation(storageManager, taskCreator);
			}

		}
	}

}
