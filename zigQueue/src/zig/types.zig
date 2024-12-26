pub const CACHE_LINE_SIZE = 64;
pub const std = @import("std");
pub const AtomicUsize = std.atomic.Value(usize);
pub const AtomicU32 = std.atomic.Value(u32);
pub const AtomicBool = std.atomic.Value(bool);
pub const AtomicSlot = std.atomic.Value(*Slot);
pub const Slot = struct {
	value: AtomicU32 = AtomicU32.init(0),
	index: u16,
	nextSlot: *Slot=undefined,
	padding2: [CACHE_LINE_SIZE - @sizeOf(*Slot) - @sizeOf(i32) - @sizeOf(AtomicU32)]u8 = undefined,
};
