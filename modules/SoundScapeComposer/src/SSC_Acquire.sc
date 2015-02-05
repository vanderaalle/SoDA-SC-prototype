

// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013-14

// this file is part of the SoundScape Composer, which has been developed as part of the Sound Design Accelerator (SoDA) project


// SSC_Acquire is designed to access the CELI's API, issue searches and requests and return the retrieved data when needed

SSC_Acquire {

	classvar htmlPreample;   // (String) the html preample string 

	var data;         // (String) the json data returned by the server
	var < isDoneCond;     // (Condition) whether the data have been downloaded
	var < isDone;     // (Bool) whether the data have been downloaded


	// ====================== initClass ====================

	*initClass {

		htmlPreample = "https://dev.celi.it/soda/api/soda/extended/dedupDocuments.json?type=search&&sortDir=-1&"; 

	}

	// ===================== set preample ==================

	setHtmlPreample { arg preample; // (String)

		case 
		{preample.isKindOf(String).not} { Error("SSC_Acquire: 'preample' argument should be an instance of String").throw }
		{preample.contains(" ") } { Error("SSC_Acquire: 'preample' String should not contain blank characters").throw };

		htmlPreample = preample; // change htmlPreample
	
	}

	// ===================== new/init =======================

	*new{
		^super.new.pr_init();
	}

	pr_init {
		isDoneCond = Condition(false);
		isDoneCond = false;
		^this;
	}

	// ==================== acquire data  ===============


	*acquireData{ arg 
		search,         // (String) the request - a string really with just the search text - individual tokens should be divided by gaps or comas
		filters = nil,        // (Array of Strings/Nil) the filters of the request
		maxNumberOfDocuments = 50, // (Integer) the maximum number of documents
		doneFunction = {};   // (Function) function to be evaluated when the data have been downloaded - the data themselves will be passed as argument ()

		^super.new.pr_init.acquireData(search,filters,maxNumberOfDocuments, doneFunction);
	}

	acquireData{ arg 
		search,         // (String) the request - a string really with just the search text - individual tokens should be divided by gaps or comas
		filters = nil,        // (Array of Strings/Nil) the filters of the request
		maxNumberOfDocuments = 50, // (Integer) the maximum number of documents
		doneFunction = {};   // (Function) function to be evaluated when the data have been downloaded - the data themselves will be passed as argument ()

		var formatedNumberOfDocuments, formatedSearch, formatedFilters;    // the carefully formated texts
		var htmlRequest;

		// type check arguments
		case 
		{search.class != String} { Error("SSC_Acquire: 'search' argument should be an instance of String").throw }
		{maxNumberOfDocuments.class != Integer} { Error("SSC_Acquire: 'maxNumberOfDocuments' argument should be an instance of Integer").throw }
		{  filters.isNil.not && (filters.class != Array) } { Error("SSC_Acquire: 'filters' argument should be an instance of Array or Nil").throw }
		{  if (filters.isKindOf(Array)) {filters.any(_.isString.not)} {false}  } { Error("SSC_Acquire: 'filters' argument should be an instance of Array containing String Objects or Nil").throw }
		{doneFunction.isKindOf(Function).not} { Error("SSC_Acquire: 'doneFunction' argument should be an instance of Function").throw };

		// prepare
		data = nil;                   // forse garbage collector to delete data (if any)
		isDoneCond = Condition(false);    // set condition to false
		isDone = false;    // set condition to false

		// prepare html request
		formatedNumberOfDocuments = "start=0&end=" ++ maxNumberOfDocuments;
		formatedSearch = "searchText=" 
		++ search.replace(" ", "+").replace(",","+"); // format search text
		htmlRequest = 
		htmlPreample ++ formatedNumberOfDocuments ++ "&" ++ formatedSearch;
		if (filters.isNil.not) {
			formatedFilters = this.pr_parseFilters(filters);
			htmlRequest = htmlRequest ++ "&" ++ formatedFilters;
		};

		// print html request (DEBUGING)
		// htmlRequest.postln;
		
		// retrieve data using the curl unix utility
		data = ("curl " ++ "\"" ++ htmlRequest ++ "\"").unixCmdGetStdOut; // quotes have to be added for correct unix formating 
		data = data.replace("\n","");
		
		// data.postln;

		// update done Condition
		isDoneCond.test_(true); 
		isDoneCond.signal;
		isDone = true;

		// call doneFunction with data as argument
		doneFunction.value(data);
	}

	// ==================== acquire data file  ===============

	*acquireDataFile { arg 
		search,         // (String) the request - a string really with just the search text - individual tokens should be divided by gaps or comas
		filters = nil,        // (Array of Strings) the filters of the request
		maxNumberOfDocuments = 50, // (Integer) the maximum number of documents
		path,           // (String/Nil) the path string to the file or nil for the default
		doneFunction;   // (Function) function to be evaluated when the data have been downloaded - nothing will be passed as an argument

		^super.new.pr_init.acquireDataFile(search,filters, maxNumberOfDocuments, path, doneFunction);
	}

	acquireDataFile { arg
		search,         // (String) the request - a string really with just the search text - individual tokens should be divided by gaps or comas
		filters = nil,        // (Array of Strings) the filters of the request
		maxNumberOfDocuments = 50, // (Integer) the maximum number of documents
		path,           // (String/Nil) the path string to the file or nil for the default
		doneFunction = {};   // (Function) function to be evaluated when the data have been downloaded - nothing will be passed as an argument
		

		var formatedNumberOfDocuments, formatedSearch, formatedFilters;    // the carefully formated texts
		var htmlRequest;

		// type check arguments
		case 
		{search.class != String} { Error("SSC_Acquire: 'search' argument should be an instance of String").throw }
		{maxNumberOfDocuments.class != Integer} { Error("SSC_Acquire: 'maxNumberOfDocuments' argument should be an instance of Integer").throw }
		{  filters.isNil.not && (filters.class != Array) } { Error("SSC_Acquire: 'filters' argument should be an instance of Array or Nil").throw }
		{  if (filters.isKindOf(Array)) {filters.any(_.isString.not)} {false}  } { Error("SSC_Acquire: 'filters' argument should be an instance of Array containing String Objects or Nil").throw }
		{doneFunction.isKindOf(Function).not} { Error("SSC_Acquire: 'doneFunction' argument should be an instance of Function").throw };

		// prepare
		data = nil;                   // forse garbage collector to delete data (if any)
		isDoneCond = Condition(false);    // set condition to false
		isDone = false;    // set condition to false

		// prepare html request
		formatedNumberOfDocuments = "start=0&end=" ++ maxNumberOfDocuments;
		formatedSearch = "searchText=" 
		++ search.replace(" ", "+").replace(",","+"); // format search text
		htmlRequest = 
		htmlPreample ++ formatedNumberOfDocuments ++ "&" ++ formatedSearch;
		if (filters.isNil.not) {
			formatedFilters = this.pr_parseFilters(filters);
			htmlRequest = htmlRequest ++ "&" ++ formatedFilters;
		};

		// print html request (DEBUGING)
		htmlRequest.postln;
		
		// retrieve data using the curl unix utility and store them on file
		("curl " ++ "\"" ++ htmlRequest ++ "\" > " ++ path.asUnixPath)
		.unixCmd({ 

			// update done Condition
			isDoneCond.test_(true); 
			isDoneCond.signal;
			isDone = true;


			// call doneFunction with data as argument
			doneFunction.value(data);

		});

	}
	
	// =================== access data ===================

	getData {
		if (isDone) {
			^data;
		} {
			"Data have not yet been acquired, please try again later".postln;
		}
	}

	getDataSynchronously {
		fork {
			isDoneCond.wait;
			^data
		}
	}

	saveData { arg path; // (String)

		var file; // the file to write to

		// type checking
		case 
		{path.isKindOf(String).not} { Error("SSC_Acquire: 'path' argument should be an instance of String representing a valid path to a file").throw }
		{ PathName(path).pathOnly.isFolder.not } { Error("SSC_Acquire: 'path' argument should be an instance of String representing a valid path to a file - this path is not valid").throw };
		
		file = File(path,"w");
		file.write(data.asString);
		file.close;

	}

	// =================== private utility functions ==========

	pr_parseFilters { arg textArray; // the text tokens to be parsed 

		var textAr = textArray;
		var text = "";                 // formated text

		// textAr = textAr.collect(_.replace(" ", "+")); // replace gaps with +
		textAr.do{ arg item;
			text = text ++  "classPath="++ item ++ "&";
		};

		text = text.drop(-1); // remove last "&"

		^text;
	}

}
