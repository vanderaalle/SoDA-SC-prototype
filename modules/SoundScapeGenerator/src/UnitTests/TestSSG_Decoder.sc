// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the SoundScape Generator, which has been developed as part of the Sound Design Accelerator (SoDA) project


TestSSG_Decoder : UnitTest {

	var decoder;

	test_chunk{
		
		decoder = SSG_Decoder.mono(pi,0,1); // 1.1 
		this.assertEquals(decoder.class, SSG_Decoder,
			"Testing object's type.", false);
		this.assertEquals(decoder.decoder.class, FoaDecoderMatrix,
			"Testing returned decoder", false);
		this.assertEquals(decoder.lfc, 1,
			"Testing lfc", false);
	}

}
