package com.activeandroid;

/*
 * Copyright (C) 2010 Michael Pardo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.activeandroid.util.IOUtils;
import com.activeandroid.util.Log;
import com.activeandroid.util.NaturalOrderComparator;
import com.activeandroid.util.SQLiteUtils;
import com.activeandroid.util.SqlParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class DatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG = DatabaseHelper.class.getSimpleName();
	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC CONSTANTS
	//////////////////////////////////////////////////////////////////////////////////////

	public final static String MIGRATION_PATH = "migrations";

	//////////////////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    //////////////////////////////////////////////////////////////////////////////////////

    private final String mSqlParser;

	//////////////////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	//////////////////////////////////////////////////////////////////////////////////////

	private Configuration configuration;

	public DatabaseHelper(Configuration configuration) {
		super(configuration.getContext(), configuration.getDatabaseName(), null, configuration.getDatabaseVersion());
		this.configuration = configuration;
		copyAttachedDatabase(configuration.getContext(), configuration.getDatabaseName());
		mSqlParser = configuration.getSqlParser();
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// OVERRIDEN METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void onOpen(SQLiteDatabase db) {
		executePragmas(db);
	};

	@Override
	public void onCreate(SQLiteDatabase db) {
		executePragmas(db);
		executeCreate(db);
		executeMigrations(db, -1, db.getVersion() == 0 ? configuration.getDatabaseVersion() : db.getVersion());
		executeCreateIndex(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		executePragmas(db);
		executeCreate(db);
		executeMigrations(db, oldVersion, newVersion);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public void copyAttachedDatabase(Context context, String databaseName) {
		final File dbPath = context.getDatabasePath(databaseName);

		// If the database already exists, return
		if (dbPath.exists()) {
			return;
		}

		// Make sure we have a path to the file
		dbPath.getParentFile().mkdirs();

		// Try to copy database file
		try {
			final InputStream inputStream = context.getAssets().open(databaseName);
			final OutputStream output = new FileOutputStream(dbPath);

			byte[] buffer = new byte[8192];
			int length;

			while ((length = inputStream.read(buffer, 0, 8192)) > 0) {
				output.write(buffer, 0, length);
			}

			output.flush();
			output.close();
			inputStream.close();
		}
		catch (IOException e) {
			Log.e("Failed to open file", e);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	private void executePragmas(SQLiteDatabase db) {
		if (SQLiteUtils.FOREIGN_KEYS_SUPPORTED) {
			db.execSQL("PRAGMA foreign_keys=ON;");
			Log.i("Foreign Keys supported. Enabling foreign key features.");
		}
	}

	private void executeCreateIndex(SQLiteDatabase db) {
		db.beginTransaction();
		try {
			for (TableInfo tableInfo : Cache.getTableInfos()) {
				String[] definitions = SQLiteUtils.createIndexDefinition(tableInfo);

				for (String definition : definitions) {
					db.execSQL(definition);
				}
			}
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
		}
	}

	private void executeCreate(SQLiteDatabase db) {
		db.beginTransaction();
		try {
			for (TableInfo tableInfo : Cache.getTableInfos()) {
				db.execSQL(SQLiteUtils.createTableDefinition(tableInfo));
			}
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
		}
	}

	private boolean executeMigrations(SQLiteDatabase db, int oldVersion, int newVersion) {
		boolean migrationExecuted = false;
		try {
			final List<String> files = Arrays.asList(Cache.getContext().getAssets().list(MIGRATION_PATH));
			Collections.sort(files, new NaturalOrderComparator());

			db.beginTransaction();
			try {
				for (String file : files) {
					try {
						final int version = getSqlVersionByFileName(file);
						if (version > oldVersion && version <= newVersion) {
							executeSqlScript(db, file);
							migrationExecuted = true;

							Log.i(file + " executed succesfully.");
						}
					}
					catch (NumberFormatException e) {
						Log.w("Skipping invalidly named file: " + file, e);
					}
				}
				db.setTransactionSuccessful();
			}
			finally {
				db.endTransaction();
			}
		}
		catch (IOException e) {
			Log.e("Failed to execute migrations.", e);
		}

		return migrationExecuted;
	}

	private int getSqlVersionByFileName(String file) {
		if(Configuration.Builder.SQL_SCRIPT_XML_FORMAT == configuration.getFormatType()){
			return Integer.valueOf(file.replace(".xml", ""));
		}
		return Integer.valueOf(file.replace(".sql", ""));
	}

	private void executeSqlScript(SQLiteDatabase db, String file) {

	    InputStream stream = null;

		try {
		    stream = Cache.getContext().getAssets().open(MIGRATION_PATH + "/" + file);

		    if (Configuration.SQL_PARSER_DELIMITED.equalsIgnoreCase(mSqlParser)) {
		        executeDelimitedSqlScript(db, stream);

		    } else if(Configuration.Builder.SQL_SCRIPT_XML_FORMAT == configuration.getFormatType()){
				executeLegacyXmlFormatSqlScript(db, stream);
			}
			else{
		        executeLegacySqlScript(db, stream);
		    }

		} catch (IOException | SAXException | ParserConfigurationException e) {
			Log.e("Failed to execute " + file, e);

		} finally {
		    IOUtils.closeQuietly(stream);

		}
	}

	private void executeDelimitedSqlScript(SQLiteDatabase db, InputStream stream) throws IOException {

	    List<String> commands = SqlParser.parse(stream);

	    for(String command : commands) {
	        db.execSQL(command);
	    }
	}

	private void executeLegacySqlScript(SQLiteDatabase db, InputStream stream) throws IOException {

	    InputStreamReader reader = null;
        BufferedReader buffer = null;

        try {
            reader = new InputStreamReader(stream);
            buffer = new BufferedReader(reader);
            String line = null;

            while ((line = buffer.readLine()) != null) {
                line = line.replace(";", "").trim();
                if (!TextUtils.isEmpty(line)) {
                    db.execSQL(line);
                }
            }

        } finally {
            IOUtils.closeQuietly(buffer);
            IOUtils.closeQuietly(reader);

        }
	}

	private void executeLegacyXmlFormatSqlScript(SQLiteDatabase db, InputStream stream) throws ParserConfigurationException, IOException, SAXException {

		String sql = null;
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.parse(stream);
		Element sqlElement = document.getDocumentElement();

		String sqlVersion = sqlElement.getAttribute("version");
		if (TextUtils.isEmpty(sqlVersion) || null == Integer.valueOf(sqlVersion)) {
			String msg = "DB " + configuration.getDatabaseName() + " attribute: 'version' :" + sqlVersion + ", shoulb be number.";
			throw new RuntimeException(msg);
		}
		try {
			android.util.Log.i(TAG, "DB " + configuration.getDatabaseName() + " ---------- Start upgrading database from " + sqlVersion + " to version " + sqlVersion);
			db.beginTransaction();

			NodeList statements = sqlElement.getElementsByTagName("statement");

			for (int i = 0; i < statements.getLength(); i++) {
				Node node = statements.item(i);
				sql = node.getTextContent();
				db.execSQL(sql);
			}
			db.setTransactionSuccessful();
			db.endTransaction();
			android.util.Log.i(TAG, "DB " + configuration.getDatabaseName() + " ---------- Finish upgrading database from " + sqlVersion + " to version " + sqlVersion);

		} catch(SQLException e) {
			android.util.Log.e(TAG, "DB " + configuration.getDatabaseName() + " Exec sql file failed, statement: " + sql, e);
		}
	}
}
