// simple SuperCollider class for detecting and interfacing with a SHBOBO deerdick.
// 
// TODO: device search assumes only one deerdick on the bus.
// TODO: more polite handling of GeneralHID, to play better with other devices.

DeerDick {
	var <period;
	var <>devlist, <>dev;
	var <>spec;
	
	*new { arg pollperiod;
		^super.new.init(pollperiod);	
	}	
	
	init {	
		period=0.005;
		
		GeneralHID.buildDeviceList;
		devlist  = GeneralHID.deviceList;
		//GeneralHID.postDevices;
				
		/*
		~dd = GeneralHID.open( ~hid_dev[2]);
		~dd.debug_( true );
		~dd.makeGui;
		~dd.info.findArgs 
		*/
		
		dev = GeneralHID.open(GeneralHID.findBy(0x6666, 0x6666));
		
		spec = GeneralHIDSpec.new( dev );
		// these are binary, should get assigned actions
		spec.add(\but0, [1, 1]);
		spec.add(\but1, [1, 2]);
		spec.add(\but2, [1, 3]);
		spec.add(\but3, [1, 4]);
		// these are floats, should get assigned .kr busses
		spec.add(\barre0, [3, 48]);
		spec.add(\barre1, [3, 49]);
		spec.add(\barre2, [3, 50]);
		spec.add(\barre3, [3, 51]);
		spec.add(\ant, [3, 52]);
	}
	
	stop {
		GeneralHID.stopEventLoop;
	}
	
	close {
		this.stop;
		dev.close;
	}
	
	start {
		GeneralHID.startEventLoop(period);
	}
	
	setPeriod { arg period;
		this.stop;
		period = period;
		this.start;
	}
}

// class to implement dickscrolling on a given deerdick barre
DeerDickScroll {
	
}


