
CfMonome
{
	var <>host, <>port, <>prefix;
	var <>responder;
	var <>addr;
	var <>rowVals;
	var <>lift, <>press;
	
	
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
					press.value(col, row);
				}
				{0}
				{
					lift.value(col, row);
				}
			}
		).add;	
		
				
		// make an array of (8bit) row values and clear them
		rowVals=Array.fill(8,0).do({arg item, i; addr.sendMsg("/box/led_row", i, 0)});
		
		
		press = { 
			arg col, row;
			"press ".post; [col, row].postln;
			this.set(col, row, 1);
		};
		
		lift = {
			arg col, row;
			"lift ".post; [col, row].postln;
			this.set(col, row, 0);
		};
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
	
	kill {
		responder.remove;
	}
}

