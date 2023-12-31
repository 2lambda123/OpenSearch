/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.remote;

import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.index.store.DirectoryFileTransferTracker;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.TestThreadPool;
import org.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RemoteSegmentTransferTrackerTests extends OpenSearchTestCase {
    private RemoteStoreStatsTrackerFactory remoteStoreStatsTrackerFactory;
    private ClusterService clusterService;

    private ThreadPool threadPool;

    private ShardId shardId;

    private RemoteSegmentTransferTracker transferTracker;

    private DirectoryFileTransferTracker directoryFileTransferTracker;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        threadPool = new TestThreadPool("remote_refresh_segment_pressure_settings_test");
        clusterService = new ClusterService(
            Settings.EMPTY,
            new ClusterSettings(Settings.EMPTY, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS),
            threadPool
        );
        remoteStoreStatsTrackerFactory = new RemoteStoreStatsTrackerFactory(clusterService, Settings.EMPTY);
        shardId = new ShardId("index", "uuid", 0);
        directoryFileTransferTracker = new DirectoryFileTransferTracker();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        threadPool.shutdownNow();
    }

    public void testGetShardId() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        assertEquals(shardId, transferTracker.getShardId());
    }

    public void testUpdateLocalRefreshSeqNo() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long refreshSeqNo = 2;
        transferTracker.updateLocalRefreshSeqNo(refreshSeqNo);
        assertEquals(refreshSeqNo, transferTracker.getLocalRefreshSeqNo());
    }

    public void testUpdateRemoteRefreshSeqNo() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long refreshSeqNo = 4;
        transferTracker.updateRemoteRefreshSeqNo(refreshSeqNo);
        assertEquals(refreshSeqNo, transferTracker.getRemoteRefreshSeqNo());
    }

    public void testUpdateLocalRefreshTimeMs() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long refreshTimeMs = System.nanoTime() / 1_000_000L + randomIntBetween(10, 100);
        transferTracker.updateLocalRefreshTimeMs(refreshTimeMs);
        assertEquals(refreshTimeMs, transferTracker.getLocalRefreshTimeMs());
    }

    public void testUpdateRemoteRefreshTimeMs() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long refreshTimeMs = System.nanoTime() / 1_000_000 + randomIntBetween(10, 100);
        transferTracker.updateRemoteRefreshTimeMs(refreshTimeMs);
        assertEquals(refreshTimeMs, transferTracker.getRemoteRefreshTimeMs());
    }

    public void testLastDownloadTimestampMs() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long currentTimeInMs = System.currentTimeMillis();
        transferTracker.getDirectoryFileTransferTracker().updateLastTransferTimestampMs(currentTimeInMs);
        assertEquals(currentTimeInMs, transferTracker.getDirectoryFileTransferTracker().getLastTransferTimestampMs());
    }

    public void testComputeSeqNoLagOnUpdate() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        int localRefreshSeqNo = randomIntBetween(50, 100);
        int remoteRefreshSeqNo = randomIntBetween(20, 50);
        transferTracker.updateLocalRefreshSeqNo(localRefreshSeqNo);
        assertEquals(localRefreshSeqNo, transferTracker.getRefreshSeqNoLag());
        transferTracker.updateRemoteRefreshSeqNo(remoteRefreshSeqNo);
        assertEquals(localRefreshSeqNo - remoteRefreshSeqNo, transferTracker.getRefreshSeqNoLag());
    }

    public void testComputeTimeLagOnUpdate() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long currentLocalRefreshTimeMs = transferTracker.getLocalRefreshTimeMs();
        long currentTimeMs = System.nanoTime() / 1_000_000L;
        long localRefreshTimeMs = currentTimeMs + randomIntBetween(100, 500);
        long remoteRefreshTimeMs = currentTimeMs + randomIntBetween(50, 99);
        transferTracker.updateLocalRefreshTimeMs(localRefreshTimeMs);
        assertEquals(localRefreshTimeMs - currentLocalRefreshTimeMs, transferTracker.getTimeMsLag());
        transferTracker.updateRemoteRefreshTimeMs(remoteRefreshTimeMs);
        assertEquals(localRefreshTimeMs - remoteRefreshTimeMs, transferTracker.getTimeMsLag());
    }

    public void testAddUploadBytesStarted() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long bytesToAdd = randomLongBetween(1000, 1000000);
        transferTracker.addUploadBytesStarted(bytesToAdd);
        assertEquals(bytesToAdd, transferTracker.getUploadBytesStarted());
        long moreBytesToAdd = randomLongBetween(1000, 10000);
        transferTracker.addUploadBytesStarted(moreBytesToAdd);
        assertEquals(bytesToAdd + moreBytesToAdd, transferTracker.getUploadBytesStarted());
    }

    public void testAddUploadBytesFailed() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long bytesToAdd = randomLongBetween(1000, 1000000);
        transferTracker.addUploadBytesFailed(bytesToAdd);
        assertEquals(bytesToAdd, transferTracker.getUploadBytesFailed());
        long moreBytesToAdd = randomLongBetween(1000, 10000);
        transferTracker.addUploadBytesFailed(moreBytesToAdd);
        assertEquals(bytesToAdd + moreBytesToAdd, transferTracker.getUploadBytesFailed());
    }

    public void testAddUploadBytesSucceeded() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long bytesToAdd = randomLongBetween(1000, 1000000);
        transferTracker.addUploadBytesSucceeded(bytesToAdd);
        assertEquals(bytesToAdd, transferTracker.getUploadBytesSucceeded());
        long moreBytesToAdd = randomLongBetween(1000, 10000);
        transferTracker.addUploadBytesSucceeded(moreBytesToAdd);
        assertEquals(bytesToAdd + moreBytesToAdd, transferTracker.getUploadBytesSucceeded());
    }

    public void testAddDownloadBytesStarted() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long bytesToAdd = randomLongBetween(1000, 1000000);
        transferTracker.getDirectoryFileTransferTracker().addTransferredBytesStarted(bytesToAdd);
        assertEquals(bytesToAdd, transferTracker.getDirectoryFileTransferTracker().getTransferredBytesStarted());
        long moreBytesToAdd = randomLongBetween(1000, 10000);
        transferTracker.getDirectoryFileTransferTracker().addTransferredBytesStarted(moreBytesToAdd);
        assertEquals(bytesToAdd + moreBytesToAdd, transferTracker.getDirectoryFileTransferTracker().getTransferredBytesStarted());
    }

    public void testAddDownloadBytesFailed() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long bytesToAdd = randomLongBetween(1000, 1000000);
        transferTracker.getDirectoryFileTransferTracker().addTransferredBytesFailed(bytesToAdd, System.currentTimeMillis());
        assertEquals(bytesToAdd, transferTracker.getDirectoryFileTransferTracker().getTransferredBytesFailed());
        long moreBytesToAdd = randomLongBetween(1000, 10000);
        transferTracker.getDirectoryFileTransferTracker().addTransferredBytesFailed(moreBytesToAdd, System.currentTimeMillis());
        assertEquals(bytesToAdd + moreBytesToAdd, transferTracker.getDirectoryFileTransferTracker().getTransferredBytesFailed());
    }

    public void testAddDownloadBytesSucceeded() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long bytesToAdd = randomLongBetween(1000, 1000000);
        transferTracker.getDirectoryFileTransferTracker().addTransferredBytesSucceeded(bytesToAdd, System.currentTimeMillis());
        assertEquals(bytesToAdd, transferTracker.getDirectoryFileTransferTracker().getTransferredBytesSucceeded());
        long moreBytesToAdd = randomLongBetween(1000, 10000);
        transferTracker.getDirectoryFileTransferTracker().addTransferredBytesSucceeded(moreBytesToAdd, System.currentTimeMillis());
        assertEquals(bytesToAdd + moreBytesToAdd, transferTracker.getDirectoryFileTransferTracker().getTransferredBytesSucceeded());
    }

    public void testGetInflightUploadBytes() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long bytesStarted = randomLongBetween(10000, 100000);
        long bytesSucceeded = randomLongBetween(1000, 10000);
        long bytesFailed = randomLongBetween(100, 1000);
        transferTracker.addUploadBytesStarted(bytesStarted);
        transferTracker.addUploadBytesSucceeded(bytesSucceeded);
        transferTracker.addUploadBytesFailed(bytesFailed);
        assertEquals(bytesStarted - bytesSucceeded - bytesFailed, transferTracker.getInflightUploadBytes());
    }

    public void testIncrementTotalUploadsStarted() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        transferTracker.incrementTotalUploadsStarted();
        assertEquals(1, transferTracker.getTotalUploadsStarted());
        transferTracker.incrementTotalUploadsStarted();
        assertEquals(2, transferTracker.getTotalUploadsStarted());
    }

    public void testIncrementTotalUploadsFailed() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        transferTracker.incrementTotalUploadsFailed();
        assertEquals(1, transferTracker.getTotalUploadsFailed());
        transferTracker.incrementTotalUploadsFailed();
        assertEquals(2, transferTracker.getTotalUploadsFailed());
    }

    public void testIncrementTotalUploadSucceeded() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        transferTracker.incrementTotalUploadsSucceeded();
        assertEquals(1, transferTracker.getTotalUploadsSucceeded());
        transferTracker.incrementTotalUploadsSucceeded();
        assertEquals(2, transferTracker.getTotalUploadsSucceeded());
    }

    public void testGetInflightUploads() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        transferTracker.incrementTotalUploadsStarted();
        assertEquals(1, transferTracker.getInflightUploads());
        transferTracker.incrementTotalUploadsStarted();
        assertEquals(2, transferTracker.getInflightUploads());
        transferTracker.incrementTotalUploadsSucceeded();
        assertEquals(1, transferTracker.getInflightUploads());
        transferTracker.incrementTotalUploadsFailed();
        assertEquals(0, transferTracker.getInflightUploads());
    }

    public void testIncrementRejectionCount() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        transferTracker.incrementRejectionCount();
        assertEquals(1, transferTracker.getRejectionCount());
        transferTracker.incrementRejectionCount();
        assertEquals(2, transferTracker.getRejectionCount());
    }

    public void testGetConsecutiveFailureCount() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        transferTracker.incrementTotalUploadsFailed();
        assertEquals(1, transferTracker.getConsecutiveFailureCount());
        transferTracker.incrementTotalUploadsFailed();
        assertEquals(2, transferTracker.getConsecutiveFailureCount());
        transferTracker.incrementTotalUploadsSucceeded();
        assertEquals(0, transferTracker.getConsecutiveFailureCount());
    }

    public void testComputeBytesLag() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );

        // Create local file size map
        Map<String, Long> fileSizeMap = new HashMap<>();
        fileSizeMap.put("a", 100L);
        fileSizeMap.put("b", 105L);
        transferTracker.updateLatestLocalFileNameLengthMap(fileSizeMap.keySet(), fileSizeMap::get);
        assertEquals(205L, transferTracker.getBytesLag());

        transferTracker.addToLatestUploadedFiles("a");
        assertEquals(105L, transferTracker.getBytesLag());

        fileSizeMap.put("c", 115L);
        transferTracker.updateLatestLocalFileNameLengthMap(fileSizeMap.keySet(), fileSizeMap::get);
        assertEquals(220L, transferTracker.getBytesLag());

        transferTracker.addToLatestUploadedFiles("b");
        assertEquals(115L, transferTracker.getBytesLag());

        transferTracker.addToLatestUploadedFiles("c");
        assertEquals(0L, transferTracker.getBytesLag());
    }

    public void testIsUploadBytesAverageReady() {
        int movingAverageWindowSize = remoteStoreStatsTrackerFactory.getMovingAverageWindowSize();
        transferTracker = new RemoteSegmentTransferTracker(shardId, directoryFileTransferTracker, movingAverageWindowSize);
        assertFalse(transferTracker.isUploadBytesAverageReady());

        long sum = 0;
        for (int i = 1; i < movingAverageWindowSize; i++) {
            transferTracker.addUploadBytes(i);
            sum += i;
            assertFalse(transferTracker.isUploadBytesAverageReady());
            assertEquals((double) sum / i, transferTracker.getUploadBytesAverage(), 0.0d);
        }

        transferTracker.addUploadBytes(movingAverageWindowSize);
        sum += movingAverageWindowSize;
        assertTrue(transferTracker.isUploadBytesAverageReady());
        assertEquals((double) sum / movingAverageWindowSize, transferTracker.getUploadBytesAverage(), 0.0d);

        transferTracker.addUploadBytes(100);
        sum = sum + 100 - 1;
        assertEquals((double) sum / movingAverageWindowSize, transferTracker.getUploadBytesAverage(), 0.0d);
    }

    public void testIsUploadBytesPerSecAverageReady() {
        int movingAverageWindowSize = remoteStoreStatsTrackerFactory.getMovingAverageWindowSize();
        transferTracker = new RemoteSegmentTransferTracker(shardId, directoryFileTransferTracker, movingAverageWindowSize);
        assertFalse(transferTracker.isUploadBytesPerSecAverageReady());

        long sum = 0;
        for (int i = 1; i < movingAverageWindowSize; i++) {
            transferTracker.addUploadBytesPerSec(i);
            sum += i;
            assertFalse(transferTracker.isUploadBytesPerSecAverageReady());
            assertEquals((double) sum / i, transferTracker.getUploadBytesPerSecAverage(), 0.0d);
        }

        transferTracker.addUploadBytesPerSec(movingAverageWindowSize);
        sum += movingAverageWindowSize;
        assertTrue(transferTracker.isUploadBytesPerSecAverageReady());
        assertEquals((double) sum / movingAverageWindowSize, transferTracker.getUploadBytesPerSecAverage(), 0.0d);

        transferTracker.addUploadBytesPerSec(100);
        sum = sum + 100 - 1;
        assertEquals((double) sum / movingAverageWindowSize, transferTracker.getUploadBytesPerSecAverage(), 0.0d);
    }

    public void testIsUploadTimeMsAverageReady() {
        int movingAverageWindowSize = remoteStoreStatsTrackerFactory.getMovingAverageWindowSize();
        transferTracker = new RemoteSegmentTransferTracker(shardId, directoryFileTransferTracker, movingAverageWindowSize);
        assertFalse(transferTracker.isUploadTimeMsAverageReady());

        long sum = 0;
        for (int i = 1; i < movingAverageWindowSize; i++) {
            transferTracker.addTimeForCompletedUploadSync(i);
            sum += i;
            assertFalse(transferTracker.isUploadTimeMsAverageReady());
            assertEquals((double) sum / i, transferTracker.getUploadTimeMsAverage(), 0.0d);
        }

        transferTracker.addTimeForCompletedUploadSync(movingAverageWindowSize);
        sum += movingAverageWindowSize;
        assertTrue(transferTracker.isUploadTimeMsAverageReady());
        assertEquals((double) sum / movingAverageWindowSize, transferTracker.getUploadTimeMsAverage(), 0.0d);

        transferTracker.addTimeForCompletedUploadSync(100);
        sum = sum + 100 - 1;
        assertEquals((double) sum / movingAverageWindowSize, transferTracker.getUploadTimeMsAverage(), 0.0d);
    }

    public void testIsDownloadBytesAverageReady() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        assertFalse(transferTracker.getDirectoryFileTransferTracker().isTransferredBytesAverageReady());

        long sum = 0;
        for (int i = 1; i < 20; i++) {
            transferTracker.getDirectoryFileTransferTracker().updateSuccessfulTransferSize(i);
            sum += i;
            assertFalse(transferTracker.getDirectoryFileTransferTracker().isTransferredBytesAverageReady());
            assertEquals((double) sum / i, transferTracker.getDirectoryFileTransferTracker().getTransferredBytesAverage(), 0.0d);
        }

        transferTracker.getDirectoryFileTransferTracker().updateSuccessfulTransferSize(20);
        sum += 20;
        assertTrue(transferTracker.getDirectoryFileTransferTracker().isTransferredBytesAverageReady());
        assertEquals((double) sum / 20, transferTracker.getDirectoryFileTransferTracker().getTransferredBytesAverage(), 0.0d);

        transferTracker.getDirectoryFileTransferTracker().updateSuccessfulTransferSize(100);
        sum = sum + 100 - 1;
        assertEquals((double) sum / 20, transferTracker.getDirectoryFileTransferTracker().getTransferredBytesAverage(), 0.0d);
    }

    public void testIsDownloadBytesPerSecAverageReady() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        assertFalse(transferTracker.getDirectoryFileTransferTracker().isTransferredBytesPerSecAverageReady());

        long sum = 0;
        for (int i = 1; i < 20; i++) {
            transferTracker.getDirectoryFileTransferTracker().addTransferredBytesPerSec(i);
            sum += i;
            assertFalse(transferTracker.getDirectoryFileTransferTracker().isTransferredBytesPerSecAverageReady());
            assertEquals((double) sum / i, transferTracker.getDirectoryFileTransferTracker().getTransferredBytesPerSecAverage(), 0.0d);
        }

        transferTracker.getDirectoryFileTransferTracker().addTransferredBytesPerSec(20);
        sum += 20;
        assertTrue(transferTracker.getDirectoryFileTransferTracker().isTransferredBytesPerSecAverageReady());
        assertEquals((double) sum / 20, transferTracker.getDirectoryFileTransferTracker().getTransferredBytesPerSecAverage(), 0.0d);

        transferTracker.getDirectoryFileTransferTracker().addTransferredBytesPerSec(100);
        sum = sum + 100 - 1;
        assertEquals((double) sum / 20, transferTracker.getDirectoryFileTransferTracker().getTransferredBytesPerSecAverage(), 0.0d);
    }

    public void testAddTotalUploadTimeInMs() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long timeToAdd = randomLongBetween(100, 200);
        transferTracker.addTotalUploadTimeInMs(timeToAdd);
        assertEquals(timeToAdd, transferTracker.getTotalUploadTimeInMs());
        long moreTimeToAdd = randomLongBetween(100, 200);
        transferTracker.addTotalUploadTimeInMs(moreTimeToAdd);
        assertEquals(timeToAdd + moreTimeToAdd, transferTracker.getTotalUploadTimeInMs());
    }

    public void testAddTotalTransferTimeMs() {
        transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            directoryFileTransferTracker,
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        long timeToAdd = randomLongBetween(100, 200);
        transferTracker.getDirectoryFileTransferTracker().addTotalTransferTimeInMs(timeToAdd);
        assertEquals(timeToAdd, transferTracker.getDirectoryFileTransferTracker().getTotalTransferTimeInMs());
        long moreTimeToAdd = randomLongBetween(100, 200);
        transferTracker.getDirectoryFileTransferTracker().addTotalTransferTimeInMs(moreTimeToAdd);
        assertEquals(timeToAdd + moreTimeToAdd, transferTracker.getDirectoryFileTransferTracker().getTotalTransferTimeInMs());
    }

    /**
     * Tests whether RemoteSegmentTransferTracker.Stats object generated correctly from RemoteSegmentTransferTracker.
     * */
    public void testStatsObjectCreation() {
        transferTracker = constructTracker();
        RemoteSegmentTransferTracker.Stats transferTrackerStats = transferTracker.stats();
        assertEquals(transferTracker.getShardId(), transferTrackerStats.shardId);
        assertEquals(transferTracker.getTimeMsLag(), (int) transferTrackerStats.refreshTimeLagMs);
        assertEquals(transferTracker.getLocalRefreshSeqNo(), (int) transferTrackerStats.localRefreshNumber);
        assertEquals(transferTracker.getRemoteRefreshSeqNo(), (int) transferTrackerStats.remoteRefreshNumber);
        assertEquals(transferTracker.getBytesLag(), (int) transferTrackerStats.bytesLag);
        assertEquals(transferTracker.getRejectionCount(), (int) transferTrackerStats.rejectionCount);
        assertEquals(transferTracker.getConsecutiveFailureCount(), (int) transferTrackerStats.consecutiveFailuresCount);
        assertEquals(transferTracker.getUploadBytesStarted(), (int) transferTrackerStats.uploadBytesStarted);
        assertEquals(transferTracker.getUploadBytesSucceeded(), (int) transferTrackerStats.uploadBytesSucceeded);
        assertEquals(transferTracker.getUploadBytesFailed(), (int) transferTrackerStats.uploadBytesFailed);
        assertEquals(transferTracker.getUploadBytesAverage(), transferTrackerStats.uploadBytesMovingAverage, 0);
        assertEquals(transferTracker.getUploadBytesPerSecAverage(), transferTrackerStats.uploadBytesPerSecMovingAverage, 0);
        assertEquals(transferTracker.getUploadTimeMsAverage(), transferTrackerStats.uploadTimeMovingAverage, 0);
        assertEquals(transferTracker.getTotalUploadsStarted(), (int) transferTrackerStats.totalUploadsStarted);
        assertEquals(transferTracker.getTotalUploadsSucceeded(), (int) transferTrackerStats.totalUploadsSucceeded);
        assertEquals(transferTracker.getTotalUploadsFailed(), (int) transferTrackerStats.totalUploadsFailed);
    }

    /**
     * Tests whether RemoteSegmentTransferTracker.Stats object serialize and deserialize is working fine.
     * This comes into play during internode data transfer.
     */
    public void testStatsObjectCreationViaStream() throws IOException {
        transferTracker = constructTracker();
        RemoteSegmentTransferTracker.Stats transferTrackerStats = transferTracker.stats();
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            transferTrackerStats.writeTo(out);
            try (StreamInput in = out.bytes().streamInput()) {
                RemoteSegmentTransferTracker.Stats deserializedStats = new RemoteSegmentTransferTracker.Stats(in);
                assertEquals(deserializedStats.shardId, transferTrackerStats.shardId);
                assertEquals((int) deserializedStats.refreshTimeLagMs, (int) transferTrackerStats.refreshTimeLagMs);
                assertEquals((int) deserializedStats.localRefreshNumber, (int) transferTrackerStats.localRefreshNumber);
                assertEquals((int) deserializedStats.remoteRefreshNumber, (int) transferTrackerStats.remoteRefreshNumber);
                assertEquals((int) deserializedStats.bytesLag, (int) transferTrackerStats.bytesLag);
                assertEquals((int) deserializedStats.rejectionCount, (int) transferTrackerStats.rejectionCount);
                assertEquals((int) deserializedStats.consecutiveFailuresCount, (int) transferTrackerStats.consecutiveFailuresCount);
                assertEquals((int) deserializedStats.uploadBytesStarted, (int) transferTrackerStats.uploadBytesStarted);
                assertEquals((int) deserializedStats.uploadBytesSucceeded, (int) transferTrackerStats.uploadBytesSucceeded);
                assertEquals((int) deserializedStats.uploadBytesFailed, (int) transferTrackerStats.uploadBytesFailed);
                assertEquals((int) deserializedStats.uploadBytesMovingAverage, transferTrackerStats.uploadBytesMovingAverage, 0);
                assertEquals(
                    (int) deserializedStats.uploadBytesPerSecMovingAverage,
                    transferTrackerStats.uploadBytesPerSecMovingAverage,
                    0
                );
                assertEquals((int) deserializedStats.uploadTimeMovingAverage, transferTrackerStats.uploadTimeMovingAverage, 0);
                assertEquals((int) deserializedStats.totalUploadsStarted, (int) transferTrackerStats.totalUploadsStarted);
                assertEquals((int) deserializedStats.totalUploadsSucceeded, (int) transferTrackerStats.totalUploadsSucceeded);
                assertEquals((int) deserializedStats.totalUploadsFailed, (int) transferTrackerStats.totalUploadsFailed);
                assertEquals(
                    (int) deserializedStats.directoryFileTransferTrackerStats.transferredBytesStarted,
                    (int) transferTrackerStats.directoryFileTransferTrackerStats.transferredBytesStarted
                );
                assertEquals(
                    (int) deserializedStats.directoryFileTransferTrackerStats.transferredBytesSucceeded,
                    (int) transferTrackerStats.directoryFileTransferTrackerStats.transferredBytesSucceeded
                );
                assertEquals(
                    (int) deserializedStats.directoryFileTransferTrackerStats.transferredBytesPerSecMovingAverage,
                    (int) transferTrackerStats.directoryFileTransferTrackerStats.transferredBytesPerSecMovingAverage
                );
            }
        }
    }

    private RemoteSegmentTransferTracker constructTracker() {
        RemoteSegmentTransferTracker transferTracker = new RemoteSegmentTransferTracker(
            shardId,
            new DirectoryFileTransferTracker(),
            remoteStoreStatsTrackerFactory.getMovingAverageWindowSize()
        );
        transferTracker.incrementTotalUploadsFailed();
        transferTracker.addTimeForCompletedUploadSync(System.nanoTime() / 1_000_000L + randomIntBetween(10, 100));
        transferTracker.addUploadBytes(99);
        transferTracker.updateRemoteRefreshTimeMs(System.nanoTime() / 1_000_000L + randomIntBetween(10, 100));
        transferTracker.incrementRejectionCount();
        transferTracker.getDirectoryFileTransferTracker().addTransferredBytesStarted(10);
        transferTracker.getDirectoryFileTransferTracker().addTransferredBytesSucceeded(10, System.currentTimeMillis());
        transferTracker.getDirectoryFileTransferTracker().addTransferredBytesPerSec(5);
        return transferTracker;
    }
}
