KeySine {
	
	var win;
	
	var <>keycodes, <>chars;
	var <>keysdown;
	var <>sine_s;
	
	var <>mousex_b, <>mousey_b;
	var <>mousex_s, <>mousey_s;
	
	var <>freqs, <>numfreqs, <>basefreq;
	
	var <>amp = 0.02;
	
	var <>s;
	
	var <>gfx_poll_r, <>gfx_poll_p = 0.2;
	
	*new {
		^super.new.init;	
	}	
	init {		
		s = Server.default;
		s.waitForBoot { 
		
		keysdown = Dictionary.new;
		
		SynthDef.new(\sine_oct, {arg out=0, amp=0.0, amplag=1.0, hz=220.0, oct=0.0, tune=0.0, tuneratio=1.015625;
//			var oct, tune;
		//	oct = MouseY.kr(0.0, 1.0, \linear); //In.kr(octbus);
		//	tune = MouseX.kr(0.0, 1.0, \linear); //In.kr(tunebus);
			tune =  tuneratio ** tune;
			Out.ar(out, Pan2.ar(Lag.kr(amp, amplag) * (SinOsc.ar(hz * tune) + (SinOsc.ar(hz * 2 * tune) * oct)), 0));
		}).store;
		
		/*
		x = Synth.new(\sine, [\hz, 220]);	
		x.set(\amp, 1.0);
		x.set(\amp, 0.0);
		*/
		
		keycodes = List[ 49, 113, 50, 119, 51, 101, 52, 114, 53, 116, 54, 121, 55, 117, 56, 105, 57, 111, 48, 112, 45, 91, 61, 93, 97, 122, 115, 120, 100, 99, 102, 118, 103, 98, 104, 110, 106, 109, 107, 44, 108, 46, 59, 47, 39 ];
		
		chars = List[$1, $q, $2, $w, $3, $e, $4, $r, $5, $t, $6, $y, $7, $u, $8, $i, $9, $o, $0, $p, $-, $[, $=, $], $a, $z, $s, $x, $d, $c, $f, $v, $g, $b, $h, $n, $j, $m, $k, $,, $l, $., $;, $/, $' ];
	
		freqs = [1, 13/12, 9/8, 6/5, 5/4, 4/3, 45/32, 3/2, 8/5, 10/6, 16/9, 24/13];
		numfreqs = freqs.size;
		basefreq = 219;
		
		sine_s = Dictionary.new;
		
		mousex_b = Bus.control(s, 1);
		mousex_s = { Out.kr(mousex_b.index, MouseX.kr(0.0, 1.0, \linear)) }.play(s);
		mousey_b = Bus.control(s, 1);
		mousey_s = { Out.kr(mousey_b.index, MouseY.kr(0.0, 1.0, \linear)) }.play(s);
		
		keycodes.do({ arg code, i;
			sine_s.add(
				code -> Synth.new(\sine_oct, [
					\out, 0,
					\amp, 0.0,
					\amplag, 4.0,
					\hz, freqs[i % (numfreqs)] * (2 ** ((i / numfreqs).floor)) * basefreq
				]);
			);
			sine_s[code].map(\oct, mousey_b);
			sine_s[code].map(\tune, mousex_b);
		});
				win = SCWindow.new;
		
		win.view.keyDownAction = ({ arg view, char, unicode, keycode; 
			if (keycodes.includes(keycode), {
				sine_s.at(keycode).set(\amp, amp);
				sine_s.at(keycode).postln;
			});
		});
		
		win.view.keyUpAction = ({ arg view, char, unicode, keycode; 
			if (keycodes.includes(keycode), {
				sine_s.at(keycode).set(\amp, 0.0);
				sine_s.at(keycode).postln;
			});
		});
		
		win.front;
		
		gfx_poll_r = Routine { inf.do({
			mousex_b.get({ arg x;
				mousey_b.get ({ arg y;
					{ win.view.background = Color.new(x*y, x, y); }.defer;
				});
			});
			gfx_poll_p.wait;
		}) }.play;
		
		
		} // /waitforboot
		
		
	} // /init
	
	tune { arg base;
		basefreq = base;
		keycodes.do({ arg code, i; sine_s.at(code).set(
			\hz, freqs[i % (numfreqs)] * (2 ** ((i / numfreqs).floor)) * basefreq;
		); });
	}
	
	
} // /Keysine