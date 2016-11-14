CfGridRaw {
	var p; // port
	var q; // queue
	var r; // rx loop

	// responder functions
	var <>keyUp, <>keyDown;
	
	*new { arg port_ = '/dev/ttyUSB0', baud_ = 115200;
		^super.new.init(port_, baud_);
	}

	init { arg port, baud;
		p = SerialPort.new(port, baud);
		q = List.new;

		r = Routine { var ch; inf.do {
			ch = p.read;
			q.add(ch);
			this.parse;
		} }.play;
		
		p.doneAction = { r.stop; };

		keyDown = { |x,y| postln("keyDown: " ++ x ++ " , " ++ y); };
		keyUp = { |x,y| postln("keyUp: " ++ x ++ " , " ++ y); };
	}
	
	parse {
		if(q.size >= 3, {
			if(q[0] == 33, { keyDown.value(q[1], q[2]); }, {
				if(q[0] == 32, { keyUp.value(q[1], q[2]); }, {
					postln("CfGridRaw: unrecognized command");
				});
			});
			q.clear;
		});
	}

	
	led_all { arg val=true;
		fork {
			p.put(val.if({ 0x13 }, { 0x12 }));
		};
	}

	led_set { arg x, y, val=true;
		fork {
			p.putAll(Int8Array([
				val.if({ 0x11 }, { 0x10 }), x, y
			]));
		};
	}

	
	led_map { arg x, y, data = Int8Array([0, 0, 0, 0, 0, 0, 0, 0]);
		fork {
			p.putAll( Int8Array.with(0x14, x, y) ++ data );
		};
	}
	
	

}