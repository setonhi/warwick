
package com.example.wa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FileResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
@Theme("wa")
public class WaUI extends UI
{

	@WebServlet(value = "/*", asyncSupported = true)
	@VaadinServletConfiguration(productionMode = false, ui = WaUI.class)
	public static class Servlet extends VaadinServlet
	{
	}



	protected File	tempFile;
	protected Table	table;

	protected int[]	varMins;
	protected int[]	varMaxs;




	private void modelInit(int sz)
	{
		varMins = new int[sz];
		varMaxs = new int[sz];

		for (int i = 0; i < sz; i++)
		{

			varMins[i] = Integer.MAX_VALUE;
			varMaxs[i] = Integer.MIN_VALUE;
		}



	}




	@Override
	protected void init(VaadinRequest request)
	{




		final VerticalLayout layout = new VerticalLayout();
		layout.setMargin(true);
		setContent(layout);



		Upload upload = new Upload("Upload CSV File", new Upload.Receiver()
		{

			@Override
			public OutputStream receiveUpload(String filename, String mimeType)
			{
				try
				{
					/* Here, we'll stored the uploaded file as a temporary file. No doubt there's
					a way to use a ByteArrayOutputStream, a reader around it, use ProgressListener (and
					a progress bar) and a separate reader thread to populate a container *during*
					the update.
					This is quick and easy example, though.
					*/
					tempFile = File.createTempFile("temp", ".csv");
					return new FileOutputStream(tempFile);
				}
				catch (IOException e)
				{
					e.printStackTrace();
					return null;
				}
			}
		});


		upload.addListener(new Upload.FinishedListener()
		{

			@Override
			public void uploadFinished(Upload.FinishedEvent finishedEvent)
			{
				try
				{
					/* Let's build a container from the CSV File */
					FileReader reader = new FileReader(tempFile);
					IndexedContainer indexedContainer = buildContainerFromCSV(reader);
					reader.close();
					tempFile.delete();

					/* Finally, let's update the table with the container */
					table.setCaption(finishedEvent.getFilename());
					table.setContainerDataSource(indexedContainer);
					table.setVisible(true);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});

		/* Table to show the contents of the file */
		table = new Table();
		table.setVisible(false);


		/* Build the main window */
		//...............................

		Button button = new Button("Upload CSV file");
		button.addClickListener(new Button.ClickListener()
		{

			public void buttonClick(ClickEvent event)
			{
				layout.addComponent(new Label("Thank you for clicking"));
			}
		});



		// Find the application directory
		String basepath = VaadinService.getCurrent().getBaseDirectory().getAbsolutePath();

		// Image as a file resource
		FileResource resource = new FileResource(new File(basepath + "/WEB-INF/images/wa.png"));

		// Show the image in the application
		Image image = new Image("The best company in the world", resource);
		layout.addComponent(image);

		Label emptyLabel2 = new Label("");
		emptyLabel2.setHeight("2em");
		layout.addComponent(emptyLabel2);

		layout.setMargin(true);
		layout.setSpacing(true);
		layout.addComponent(table);

		Label emptyLabel3 = new Label("");
		emptyLabel3.setHeight("2em");
		layout.addComponent(emptyLabel3);

		layout.addComponent(upload);

	}




	/**
	 * Read the entire contents of a CSV
	 * file, and creates an IndexedContainer from it
	 *
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	protected IndexedContainer buildContainerFromCSV(Reader reader) throws IOException
	{
		IndexedContainer container = new IndexedContainer();


		String[] columnHeaders = null;
		String[] record;


		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";

		List<String[]> unfilteredRecords = new ArrayList();

		try
		{

			br = new BufferedReader(new FileReader(tempFile));

			while ((line = br.readLine()) != null)
			{

				// use comma as separator
				record = line.split(cvsSplitBy);

				if (columnHeaders == null)
				{
					columnHeaders = record;
					addItemProperties(container, columnHeaders);

					modelInit(record.length - 2);//Next coding iteration we need to check record integrity!!
				}
				else
				{
					//addItem(container, columnHeaders, filter(record));
					modelUpdate(record);
					unfilteredRecords.add(record);
				}

			}

		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
		finally
		{
			if (br != null)
			{
				try
				{
					br.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
					return null;

				}
			}
		}


		for (String[] rec : unfilteredRecords)
		{

			String[] filtered = filter(rec);
			if (filtered != null)
				addItem(container, columnHeaders, filter(filtered));
		}

		return container;
	}




	private void modelUpdate(String[] record)
	{
		// TODO Add check that varnames order is correct or else allow for any ordering
		// Assuming for now data format is good 


		int decision = Integer.parseInt(record[record.length - 1]); // Assuming decision is last field. Improve next iteration



		if (decision == 0)
			return;



		int[] vals = new int[record.length - 2];

		for (int i = 1; i < (record.length - 1); i++)
		{
			try
			{
				int varval = Integer.parseInt(record[i]);
				vals[i - 1] = varval;
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				return;
			}
			catch (NumberFormatException e)
			{
				return;
			}

		}

		for (int i = 0; i < vals.length; i++)
		{
			int varvalue = vals[i];

			if (varvalue < varMins[i])
			{
				varMins[i] = varvalue;
			}

			if (varvalue > varMaxs[i])
			{
				varMaxs[i] = varvalue;
			}
		}



	}




	private String[] filter(String[] record)
	{

		int[] vals = new int[record.length - 2];

		for (int i = 1; i < (record.length - 1); i++)
		{
			vals[i - 1] = Integer.parseInt(record[i]);

		}


		int decision = Integer.parseInt(record[record.length - 1]); // Assuming decision is last field. Improve next iteration


		int failcount = 0;
		int succeedcount = 0;

		for (int i = 0; i < vals.length; i++)
		{
			if (((vals[i] < varMins[i]) || (vals[i] > varMaxs[i])) && (decision == 0))
			{
				failcount++;
			}
			else
			{
				succeedcount++;

			}
		}

		if (succeedcount == 0)
		{
			return null;
		}
		else
		{
			return record;
		}
	}




	/**
	 * Set's up the item property ids for the container. Each is a String (of course,
	 * you can create whatever data type you like, but I guess you need to parse the whole file
	 * to work it out)
	 *
	 * @param container
	 *            The container to set
	 * @param columnHeaders
	 *            The column headers, i.e. the first row from the CSV file
	 */
	private static void addItemProperties(IndexedContainer container, String[] columnHeaders)
	{
		for (String propertyName : columnHeaders)
		{
			container.addContainerProperty(propertyName, String.class, null);
		}
	}




	/**
	 * Adds an item to the given container, assuming each field maps to it's corresponding property id.
	 * Again, note that I am assuming that the field is a string.
	 *
	 * @param container
	 * @param propertyIds
	 * @param fields
	 */
	private static void addItem(IndexedContainer container, String[] propertyIds, String[] fields)
	{
		if (propertyIds.length != fields.length)
		{
			throw new IllegalArgumentException("Hmmm - Different number of columns to fields in the record");
		}
		Object itemId = container.addItem();
		Item item = container.getItem(itemId);
		for (int i = 0; i < fields.length; i++)
		{
			String propertyId = propertyIds[i];
			String field = fields[i];
			item.getItemProperty(propertyId).setValue(field);
		}
	}
}