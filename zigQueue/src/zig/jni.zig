const std = @import("std");
const t = @import("types.zig");
const Queue = @import("busywait_queue.zig").BusyWaitQueue;
pub export fn AsyncZigConsumer_initQueue(size: i32) i64 {
	var qp: usize = 0;
	var thread = std.Thread.spawn(.{}, Monitor, .{ size, &qp }) catch {
		std.debug.print("\nCant start monitor thread", .{});
		return 0;
	};
	while (qp == 0) {
		std.time.sleep(100);
	}
	const q: *Queue = @ptrFromInt(qp);
	q.*.holder = &thread;
	@fence(.acq_rel);
	return @intCast(qp);
}
pub fn Monitor(size: i32, qOut: *usize) void {
	var gpa = std.heap.GeneralPurposeAllocator(.{}){};
	const allocator = gpa.allocator();
	var q: Queue = Queue.init(allocator, @intCast(size)) catch |err| {
		std.debug.print("\n Monitor: can't create thread {}", .{err});
		return;
	};
	qOut.* = @intFromPtr(&q);
	@fence(.acq_rel);
	while (!q.stopFlag.load(.acquire)) {
		std.time.sleep(1000_000_000);
		// var write = q.sharedWriteSlot.load(.acquire);
		// var read = q.sharedReadSlot.load(.acquire);
		// std.debug.print("\n Monitor: queue: {%p}, writeSlot {}:{}, readSlot {}:{}", .{ &&q, write.index, write.value.load(.acquire), read.index, read.value.load(.acquire) });
    }
	q.deinit();
}
pub export fn AsyncZigConsumer_writeNative(cQueue: i64, value: i32) void {
	const queue = @as(*Queue, @ptrFromInt(@as(usize, @intCast(cQueue))));
	queue.write(@intCast(value));
}

pub export fn AsyncZigConsumer_readNative(cQueue: i64) i32 {
	const queue = @as(*Queue, @ptrFromInt(@as(usize, @intCast(cQueue))));
	const value: i32 = @intCast(queue.read());
	return value;
}

pub export fn AsyncZigConsumer_deinitQueue(cQueue: i64) void {
	const queue = @as(*Queue, @ptrFromInt(@as(usize, @intCast(cQueue))));
	queue.stopFlag.store(true, .release);
}
