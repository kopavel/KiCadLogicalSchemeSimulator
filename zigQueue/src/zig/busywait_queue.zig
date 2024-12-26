const std = @import("std");
const types = @import("types.zig");

pub const BusyWaitQueue = struct {
	allocator: std.mem.Allocator,
	sharedWriteSlot: types.AtomicSlot,
	sharedReadSlot: types.AtomicSlot,
	writeSlot: *types.Slot,
	readSlot: *types.Slot,
	data: []types.Slot,
	holder: *std.Thread = undefined,
	stopFlag: types.AtomicBool = types.AtomicBool.init(false),

	pub fn init(allocator: std.mem.Allocator, capacity: u32) !BusyWaitQueue {
		const slots: []types.Slot = try allocator.alloc(types.Slot, capacity);
		for (0..capacity) |idx| {
			slots[idx] = types.Slot{ .index = @intCast(idx) };
			if (idx == capacity - 1) {
				slots[idx].nextSlot = &slots[0];
			} else {
				slots[idx].nextSlot = &slots[idx + 1];
			}
		}
		@fence(.acq_rel);
		return BusyWaitQueue{
			.allocator = allocator,
			.sharedWriteSlot = types.AtomicSlot.init(&slots[0]),
			.writeSlot = &slots[0],
			.sharedReadSlot = types.AtomicSlot.init(&slots[0]),
			.readSlot = &slots[0],
			.data = slots,
		};
	}

	pub fn write(self: *BusyWaitQueue, value: u32) void {
		const curSlot = self.writeSlot;
		const nextSlot = curSlot.nextSlot;
		// std.debug.print("\nwrite to slot {}, next are {}", .{ curSlot.index, nextSlot.index });
        while (nextSlot == self.sharedReadSlot.load(.acquire)) {}
		// std.debug.print("\nwrite done", .{});
        curSlot.value.store(value, .release);
		self.writeSlot = nextSlot;
		self.sharedWriteSlot.store(nextSlot, .release);
		// std.debug.print("\nwrite pointer on {}", .{nextSlot.index});
    }

	pub fn read(self: *BusyWaitQueue) u32 {
		const curSlot = self.readSlot;
		const nextSlot = curSlot.nextSlot;
		// std.debug.print("\nread from slot {}", .{curSlot.index});
        while (curSlot == self.sharedWriteSlot.load(.acquire)) {}
		const readVal = curSlot.value.load(.acquire);
		// std.debug.print("\nread done: {}", .{readVal});
        self.readSlot = nextSlot;
		self.sharedReadSlot.store(nextSlot, .release);
		return readVal;
	}

	pub fn is_empty(self: *BusyWaitQueue) bool {
		return self.sharedReadSlot.load(.acquire) == self.sharedWriteSlot.load(.acquire);
	}

	pub fn is_full(self: *BusyWaitQueue) bool {
		return self.sharedWriteSlot.load(.acquire).nextSlot == self.sharedReadSlot.load(.acquire);
	}

	pub fn deinit(self: *BusyWaitQueue) void {
		//std.debug.print("\n !!!deinit!!!", .{});
        self.sharedWriteSlot.store(&self.data[0], .release);
		self.sharedReadSlot.store(&self.data[self.data.len / 2], .release);
		self.allocator.free(self.data);
		//std.debug.print("\n !!!deinit done!!!", .{});
    }
};
