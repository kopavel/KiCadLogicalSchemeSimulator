const std = @import("std");
const types = @import("types.zig");
const jni = @import("jni.zig");
const busywait_queue = @import("busywait_queue.zig");
// Create an atomic boolean for the stop signal using std.atomic.Value

pub fn Producer(queue: *busywait_queue.BusyWaitQueue) void {
	for (1..300_000_001) |i| {
		queue.write(@intCast(i)); // Write data to the queue
    }

	queue.write(0); // Write data to the queue
}

pub fn Consumer(queue: *busywait_queue.BusyWaitQueue) void {
	var value: u32 = 1;
	while (value != 0) {
		value = queue.read();
	}
}

pub fn main() !void {
	const stdout = std.io.getStdOut().writer();
	var gpa = std.heap.GeneralPurposeAllocator(.{}){};
	const allocator = gpa.allocator();

	var queue = try busywait_queue.BusyWaitQueue.init(allocator, @intCast(1000));
	// var queue2 = try busywait_queue.BusyWaitQueue.init(allocator);
	// var queue3 = try busywait_queue.BusyWaitQueue.init(allocator);
	// var queue4 = try busywait_queue.BusyWaitQueue.init(allocator);

	const start_time = std.time.milliTimestamp();

	const producer_thread = try std.Thread.spawn(.{}, Producer, .{&queue}); // config first, then function, then args
    const consumer_thread = try std.Thread.spawn(.{}, Consumer, .{&queue}); // config first, then function, then args
    // const consumer_thread2 = try std.Thread.spawn(.{}, Consumer, .{&queue2}); // config first, then function, then args
    // const consumer_thread3 = try std.Thread.spawn(.{}, Consumer, .{&queue3}); // config first, then function, then args
    // const consumer_thread4 = try std.Thread.spawn(.{}, Consumer, .{&queue4}); // config first, then function, then args

	producer_thread.join();
	consumer_thread.join();
	// consumer_thread2.join();

	const end_time = std.time.milliTimestamp();
	const elapsed_time_ms = @as(f64, @floatFromInt(end_time - start_time));
	const milionPerSec = std.math.round(300_000 / elapsed_time_ms);

	try stdout.print("{d}M ops/sec\n", .{milionPerSec});

	queue.deinit();
}
