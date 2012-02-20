
Cf40h
{
	var <>host, <>port, <>prefix;
	var <>responder;
	var <>addr;
	var <>rowVals;
	var <>press, <>lift;
	var <>liftFunctions, <>pressFunctions;
	
	
	*new {
		arg host = "127.0.0.1", port = 8080, prefix='/40h';
		^super.newCopyArgs(host, port, prefix).init(host, port, prefix);
	}
		
	init {	
		arg hostArg, portArg, prefixArg;

/*		
postln("args:");
postln(host);
postln(port);
postln(prefix);
*/
		host = hostArg; 
		port = portArg;
		prefix = prefixArg;
		
		addr = NetAddr(host, port);
		
		if (prefix.isNil, {prefix = '/40h'; });
		postln(prefix ++ '/press');
		
		responder = OSCresponder 
		(	nil, prefix ++ '/press',
			{	arg time, responder, msg;
				var string, col, row, val;
				# string, col, row, val = msg;
				switch (val) 
				{1}
				{
					pressFunctions.do({ arg func; func.value(col, row); });
					// backwards compatibility
					press.value(col, row);
				}
				{0}
				{
					liftFunctions.do({ arg func;  func.value(col, row); });
					// backwards compatibility
					lift.value(col, row);
				}
			}
		).add;	
		
				
		// make an array of (8bit) row values and clear them
		rowVals=Array.fill(8,0).do({arg item, i; addr.sendMsg("/box/led_row", i, 0)});
		
		press = { 
			arg col, row;
			"press ".post; [col, row].postln;
			this.led(col, row, 1);
			
		};
		
		lift = {
			
			arg col, row;
			"lift ".post; [col, row].postln;
			this.led(col, row, 0);
			
		};
		
		//pressFunctions = Dictionary.new.add('default'->press);
		//liftFunctions = Dictionary.new.add('default'->lift);
		pressFunctions = Dictionary.new;
		liftFunctions = Dictionary.new;
	}
	
	setPrefix { arg prefixArg;
		this.init(host, port, prefixArg);
		
	}
	
	// always use /box/led_row and bitmasking cause it's faster for the 40h hardware (?)
	led {
		arg x, y, v;
//		addr.sendMsg(prefix ++ '/led', x, y, v);
		var val;
		if (v>0, {val=(rowVals[y]) | (1 << x)},{val=(rowVals[y] & (1 << x).bitNot)});
		this.ledRow(y, val);
	}
	
	ledRow {	arg y, val;
		addr.sendMsg(prefix ++ '/led_row', y, val);
		rowVals[y] = val;
	}
	
	emu {
	//	win = 
	}
	
	kill {
		responder.remove;
	}
	
	clear {
		8.do({|row|this.ledRow(row, 0); });
	}

}

// class to represent a square of buttons as one big button
CfQuad {
	var <size, <>state, <>quadState, <>toggle, <>onAction, <>offAction;
	*new {
		arg size=4;	
		^super.new.init(size);
	}
	init { arg sizeArg;
		size = sizeArg;
		state = Array.fill(size * size, {0});
		quadState = 0;
		toggle = false;
		onAction = {};
		offAction = {};
	}
	press { arg x, y;
		if(toggle, {
			if (state.every({|v| v==0 }), {
				// quadState.postln;
				quadState = 1 - quadState;
				if (quadState == 1, {
					// postln("on");
					onAction.value();
				}, {
					// postln("off");
					offAction.value();
				});
			});
		}, {	// momentary
			if (quadState == 0, {
				quadState = 1;
//				postln("on");
				onAction.value();
			});
		});
		state[x*size + y] = 1;
	}
	
	lift { arg x, y;
		state[x*size + y] = 0;
		if(toggle, {
			// lift never toggles
		}, {
			if (quadState == 1, {
				if(state.every({|v| v==0 }), {
					quadState = 0;
					// postln("off");
					offAction.value();
				});
			});
		});
	}
}
