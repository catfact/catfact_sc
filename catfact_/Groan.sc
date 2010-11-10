Groan {
	var s;
	var <window, <view, <width, <height;
	var <bufs;
	var numbufs=8, buflen=60.0;
	var loopWatchR, loopWatchF, loopDef;
	
	*new{ arg server;
		^super.new.init(server);
	}
	
	init {
	arg server;
		s = server;
		s.waitForBoot { Routine {
			bufs = Array.fill(8, { Buffer.alloc(s, s.sampleRate * buflen, 1); });
		}.play }
	}
}