FourTap {

	var s;
	var pbus;
	var pmouse;
	var del_buf_l, del_buf_r;
	var del_in_b, del_in_s, del_s;
	var del_tap;
	var win;
	var del_tap_chars;
	var tapmode;
	var thex, they;	
	var pollmouse, pollinterval;
	
	var inputmute = false;
	
	*new {
		arg server;
		^super.new.init(server);
	}

	init {
		arg server;
		s = server;	
		
		Routine {		
				
			SynthDef.new(\adc_patch, {arg in=0, out=0, amp=1, amplag = 0.01;
				var sig;
				amp = Lag.kr(amp, amplag);
				sig = SoundIn.ar(in);
				Out.ar(out, sig * amp);
			}).send(s);
			
			s.sync;
							
			SynthDef.new(\bufd_svf, {
				arg in=0, out=0, buf=0, amp=1.0,
				delaytime=1.0,
				hzbus=0, hzmul=1.0, hzadd=0.0, hzdelay=0.0,
				hzlag=0.01,
				res=0.0,
				low=1.0, band=1.0, high=0.0, notch=0.0, peak=0.0,
				fb=0.0;
				
				var input, hz, del, output;
				
				fb = fb.min(0.8);
				
				input = In.ar(in) + (LocalIn.ar(1) * fb);
				del = BufDelayL.ar(buf, input, delaytime);
				
				hz = Lag.kr(In.kr(hzbus), hzlag, hzmul, hzadd);
				hz = DelayL.kr(hz, 10.0, hzdelay);
				
				output = SVF.ar(del, hz, res, low, band, high, notch, peak);
				//output = del;
				
				LocalOut.ar(output);
				Out.ar(out, output * amp);
			}).send(s);
		
			s.sync;			
			
			// param control
			pbus = Array.fill(4, { Bus.control(s, 1); });
			pmouse = Array.fill(4, {|i| Synth.new(\mousefreeze, [\out, pbus[i].index], s, \addToHead);  });
			pmouse[0].set(\xy, 1);
			pmouse[1].set(\xy, 0);
			pmouse[2].set(\min, 30.0, \max, 8000.0, \warp, 1.0, \xy, 1);
			pmouse[3].set(\xy, 0, \min, 0.0, \max, 0.05);
			
			// echo memory 4 x 2 mono buffers
			del_buf_l = Array.fill(4, { Array.fill(2, { Buffer.alloc(s, 120.0 * s.sampleRate, 1); }); });
			
			// 2x mono input bus
			del_in_b = Array.fill(2, { Bus.audio(s, 1); });
			del_in_s = Array.fill(2, { arg i; Synth.new(\adc_patch, [\in, i, \out, del_in_b[i].index], s) });
			
			// 4 x 2 mono delay->svf->fb synths
			del_s = Array.fill(4, { arg i; Array.fill(2, { arg j;
				Synth.new(\bufd_svf, [
				\in, del_in_b[j].index, \out, j, \buf, del_buf_l[i][j].bufnum], s, \addToTail)
			}); });
			del_s.do({ arg syns, i; 
				syns.do({ arg syn;
					syn.set(\band, 0.0);
					syn.map(\amp, pbus[0]);
					syn.map(\fb, pbus[1]);
					syn.set(\hzbus, pbus[2].index);
					syn.map(\res, pbus[3]);
				});
			});
			
			del_tap = Array.fill(4, { Array.fill(2, { CfTapper.new.tap; }); });
			
			///////////// ui
			
			AppClock.sched(0.0, {
				win = SCWindow.new("___...___", SCWindow.screenBounds, border:false).front;
				win.view.background = Color(0);
				
				del_tap_chars = [[$1, $q], [$2, $w], [$3, $e], [$4, $r]];
				
				tapmode = nil; 
				
				win.view.keyDownAction = { 
					arg view, char, mod, uni, key;
					
					[char, mod, key].postln;
					
					if (uni == 32, {
						if (inputmute, {
							inputmute = false;
							del_in_s.do({arg syn; syn.run(true);});
							win.refresh;
						}, {
							inputmute = true;
							del_in_s.do({arg syn; syn.run(false);});
							win.refresh;
						});
					});
					
					del_tap_chars.do({ arg pair, i;
						pair.do({ arg thechar, j;
				//			("checking " ++ code).postln;
							if(char == thechar, {
								del_tap[i][j].tap;
								if (tapmode == \release, {
								}, {
									del_s[i][j].set(\delaytime, del_tap[i][j].time); 
								});
							
				//				("tapping" ++ i ++ ", " ++ j).postln;
							});
						});
					});
				};
				
				win.view.keyUpAction = { 
					arg view, char, mod, uni, key;
					
					[char, mod, key].postln;
					
					del_tap_chars.do({ arg pair, i;
						pair.do({ arg thechar, j;
				//			("checking " ++ code).postln;
							if(char == thechar, {				
								if (tapmode == \release, {
									del_tap[i][j].tap;
									del_s[i][j].set(\delaytime, del_tap[i][j].time);
								});
				//				("tapping" ++ i ++ ", " ++ j).postln;
							});
						});
					});
				};
				
				
				////////////// gfx
				
				
				thex = 0.0;
				they = 0.0;
				
				win.drawHook = {
					var width, height;
					width = win.view.bounds.width;
					height = win.view.bounds.height;
				//	postln("" ++ width ++ " , " ++ height);
				//	thex.postln;
				//	they.postln;
					win.view.background = Color.new(thex, thex, thex);
					Pen.fillColor = Color.new(they, they, they);
					Pen.addWedge(Point(width * 0.5, height* 0.5), height * 0.5, 0, 2*pi);
					Pen.perform(\fill);
					if (inputmute, {
						Pen.fillColor = Color.red;
						Pen.font = Font("Helvetica", 72);
						Pen.stringAtPoint("INPUT MUTE", 0@0);
					}); 
				};
				
				
				pollinterval = 0.1;
				
				pollmouse = Routine { inf.do {
					arg i;
					
					pbus[0].get({
						arg valx;
						thex = valx;
						pbus[1].get({
							arg valy;
							they = valy;
							
							/* update gfx */
							AppClock.sched(0, {
								win.refresh;
							nil });
							
						});
					});
					
					pollinterval.wait;
				}};
				
				pollmouse.play;
				/*
				pollmouse.stop;
				*/
				
				//CmdPeriod.removeAll;
				CmdPeriod.doOnce({ win.close });
				
				win.front;
				win.fullScreen;
			nil}); // appclock sched
		}.play; // init routine
	} // init func
} // class