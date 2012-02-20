CfTapper {
	var <>now, <>time;

	*new {
		^super.new.init;
	}

	init {
		now = 0.0;
		time = 0.0;
	}

	tap {
		var thenow;
		thenow = SystemClock.seconds;
		if (now.notNil, {
			time = thenow - now;
			now = thenow;
		}, { 
			time = 0.0;
			now = thenow;
		});
		^time
	}

	reset {
		now = 0.0;
		time = 0.0;
	}
}