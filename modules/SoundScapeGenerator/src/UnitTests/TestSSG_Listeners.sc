// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the SoundScape Generator, which has been developed as part of the Sound Design Accelerator (SoDA) project

// SSG_SoundZone unit tests


TestSSG_AbstractListener : UnitTest {

	var listener;
	var arrayOfListeners, arrayOfIds;  // used for the uniqueID test
	

	setUp {
		listener = SSG_AbstractListener(inf,\aListener,"no text");
	}

	test_chunk {
		"Testing object's type.".postln;
		this.assertEquals(listener.class, SSG_AbstractListener,
			"Testing object's type.", false);

		"Testing if member variables have been initialized correctly".postln;
		this.assertEquals( listener.label, \aListener, "Testing if 'label' (instance variable) has been initialized correctly.", false);
		this.assertEquals( listener.info, "no text", "Testing if 'label' (instance variable) has been initialized correctly.", false);
		this.assertEquals( listener.listeningRadius, inf, "Testing if 'label' (instance variable) has been initialized correctly.", false);

		"Testingy setters".postln;
		listener.setLabel(\anotherLabel);
		this.assertEquals( listener.label, \anotherLabel, "Testing if 'label' (instance variable) has been set correctly.", false);
		listener.setInfo("other kind of text");
		this.assertEquals( listener.info, "other kind of text", "Testing if 'info' (instance variable) has been set correctly.", false);
		listener.setListeningRadius(15);
		this.assertEquals( listener.listeningRadius, 15, "Testing if 'listeningRadius' (instance variable) has been set correctly.", false);

		"Testing position bus' type".postln;
		this.assertEquals( listener.position.class, CtkControl, "Testing if 'position' control bus is of correct type.", false);
		"You should now see polling data from the position bus".postln;
		Server.default.waitForBoot({
			{In.kr(listener.position,3).poll * Line.kr(1,1,4,doneAction:2)}.play;
		});
		
		"Testing if automatically generated ids are trully unique".postln;
		arrayOfListeners = Array.fill(100,{SSG_AbstractListener.new});
		arrayOfIds = arrayOfListeners.collect(_.id);
		this.assertEquals( arrayOfIds.as(IdentitySet).size, 100, "Testing if automatically generate ids are trully unique.", false);
	}
}


TestSSG_FixedListener : UnitTest {

	var listener;
	var arrayOfListeners, arrayOfIds;  // used for the uniqueID test
	

	setUp {
		listener = SSG_FixedListener(15@43@30,inf,\aListener,"no text");
	}

	test_chunk {
		"You should now see polling data from the position bus, make sure they stand for the Cartesian point 15@43@30 after 4 seconds turning to 20@20@20".postln;
		Server.default.waitForBoot({
			
			listener.play;
			{In.kr(listener.position,3).poll * Line.kr(1,1,8,doneAction:2); Silent.ar(1)}.play;
			
			fork{
				4.wait;
				listener.setNewPosition(20@20@20);
			}
		});
	}

}