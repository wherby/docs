  downloadCSV = (csv, filename) => {
    var csvFile;
    var downloadLink;

    // CSV file
    csvFile = new Blob([csv], {type: 'text/csv;charset=utf-8;'});



    if (window.navigator.msSaveOrOpenBlob){
      // IE hack; see http://msdn.microsoft.com/en-us/library/ie/hh779016.aspx
      window.navigator.msSaveBlob(csvFile, filename);
    }
    else
    {
    // Download link
    downloadLink = document.createElement("a");

    // File name
    downloadLink.download = filename;

    // Create a link to the file
    downloadLink.href = window.URL.createObjectURL(csvFile);
    console.log(downloadLink.href)

    // Hide download link
    downloadLink.style.display = "none";

    // Add the link to DOM
    document.body.appendChild(downloadLink);

    // Click download link
    downloadLink.click();

    //document.body.removeChild(downloadLink);
  }
  }