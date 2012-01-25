//// a granular synth which simply exposes all its grain parameters as control busses
//// takes a running server and a source buffer as creation args

BusGrainer {
	var <>s,
	<>buf,
	<>gr_s,
	<>grtrig_b,
	<>grpos_b,
	<>grdur_b,
	<>grrate_b,
	<>grpan_b,
	<>grtrig_s,
	<>grpos_s,
	<>grdur_s,
	<>grrate_s,
	<>grpan_s,
	<>grainposrate;
		
	*new{ arg server, buffer, target, out;
		^super.new.init(server, buffer, target, out);
	}
	
	init { arg server, buffer, target, out;
		
		s = server;
		buf = buffer;
		
		Routine {	
			//default: 1/8th speed position
			grainposrate = 0.125;
			
			SynthDef.new(\krbuspangrain, {
				arg buf=0, out=0, amp=0.25,
					trig_bus = 0,
					pos_bus = 1,
					dur_bus = 2,
					rate_bus = 3,
					pan_bus = 4,
					maxgrains = 64,
					env_buf = -1,
					amplag=4.0;
			
				var trig, pos, dur,
					rate, pan,
					snd;
				
				trig = In.kr(trig_bus);
				pos = In.kr(pos_bus);
				dur = In.kr(dur_bus);
				rate = In.kr(rate_bus); 
				pan = In.kr(pan_bus);
				
				
				amp = Lag.kr(amp, amplag);
				
				snd = GrainBuf.ar(
					numChannels:2,
					trigger:trig, 
					dur:dur, 
					sndbuf:buf, 
					rate:rate, 
					pos:pos, 
					interp:2, 
					pan:pan, 
					envbufnum:env_buf, 
					maxGrains:maxgrains, 
					mul:amp);
				
				Out.ar(out, snd);
			}).send(s);

							
			grtrig_b = Bus.control(s, 1);
			grpos_b = Bus.control(s, 1);
			grdur_b = Bus.control(s, 1);
			grrate_b = Bus.control(s, 1);
			grpan_b = Bus.control(s, 1);
			
			gr_s = Synth.new(\krbuspangrain, [
				\out, out,
				\buf, buf.bufnum,
				\trig_bus, grtrig_b.index,
				\pos_bus, grpos_b.index,
				\dur_bus, grdur_b.index,
				\rate_bus, grrate_b.index,
				\pan_bus, grpan_b.index
			], target);
			
			// default: impulse for trig
			grtrig_s = SynthDef.new(\krbusgraintrigpulse, {
				arg out=0, hz=8.0;
				Out.kr(out, Impulse.kr(hz));
			}).play(gr_s, [\out, grtrig_b.index], \addBefore);
			
			// default: sawtooth for pos
			grpos_s = SynthDef.new(\krbusgrainpossaw, {
				arg out=0, hz=0.01, iphase=0;
				Out.kr(out, LFSaw.kr(hz, iphase, 0.5, 0.5));
			}).play(gr_s, [\out, grpos_b.index, \hz, grainposrate * 1.0 / buf.duration], \addBefore);
			
			
			s.sync;
			
			grdur_b.set(0.5);
			grrate_b.set(1.0);
			grpan_b.set(0.0);
			
			// default grainposrate
			grainposrate = 0.1;
			grpos_s.set(\hz, grainposrate * 1.0 / buf.duration);	
			// default trigger rate
			grtrig_s.set(\hz, 4.0);
		}.play; 		
	}
	
	kill {
		gr_s.free;
		grtrig_s.free;
		grpos_s.free;
		grdur_s.free;
		grrate_s.free;
		grpan_s.free;
		
		grtrig_b.free;
		grpos_b.free;
		grdur_b.free;
		grrate_b.free;
		grpan_b.free;
	}

} 