package com.dev.ah10.androidratclient;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

class Contacts
{
    private Context ctx; //The context to use in the class

    Contacts(Context appContext) //Constructor
    {
        //Set the class global variables
        ctx = appContext;
    }

    Integer[] getContactIds() //Get a list of contact IDs
    {
        List<Integer> returnList = new ArrayList<>(); //Create the id list
        ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null); //Create a new query

        if (cur != null && cur.getCount() > 0) //If can query, at least 1 row exists
        {
            while (cur.moveToNext()) //Move to the next row in table
            {
                String contactID = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID)); //Get the id of the contact
                Integer id = Integer.parseInt(contactID); //Parse the id to integer
                returnList.add(id); //add id to the list
            }

            cur.close(); //Close the query
        }

        //Convert list to Array
        Integer[] returnArray = new Integer[returnList.size()];
        returnList.toArray(returnArray);
        return returnArray; //Return the array of IDs
    }

    String getContactName(Integer contactID) //Get the name of the contact by ID
    {
        ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
        String strID = String.valueOf(contactID); //Convert id to string
        Cursor cur = cr.query( //Create the query
                ContactsContract.Contacts.CONTENT_URI, //Contacts Table
                null,
                ContactsContract.Contacts._ID + " = ?", //Where id = ?
                new String[] {strID}, //Replace ? with the id (prevent's sql Injection)
                null);

        if (cur != null && cur.getCount() > 0) //If can query, at least 1 row is in table
        {
            cur.moveToFirst(); //Move to the first row, that matches the criteria
            String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)); //Get the contact name
            cur.close(); //Close the query
            return name; //Return the contact name
        }

        return "N/A"; //Contact name not found
    }

    String[] getContactPhoneNumbers(Integer contactID) //Get the phone number of the contact by id
    {
        ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
        List<String> phoneNumbers = new ArrayList<>(); //Define a list of phone numbers (1 contact can have multiple numbers)
        String strID = String.valueOf(contactID); //Convert id to string
        Cursor cur = cr.query( //Create a query
                ContactsContract.Contacts.CONTENT_URI, //Contacts Table
                null,
                ContactsContract.Contacts._ID + " = ?", //Where id = ?
                new String[] {strID}, //Replace ? with the ID (prevent's sql injection)
                null);
        if (cur != null && cur.getCount() > 0) //If can query, data available
        {
            cur.moveToFirst(); //Move to the first match

            if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) //if contact has at least 1 number
            {
                Cursor phoneCur = cr.query( //Create another query
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, //Phone Number Table
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", //Where contact id = ?
                        new String[] {strID}, //replace ? with ID of contact (prevent's sql injection)
                        null
                );

                if (phoneCur != null && phoneCur.getCount() > 0) //if can query, data available
                {
                    while (phoneCur.moveToNext()) //Move to the next row
                    {
                        String number = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)); //Get the phone number
                        phoneNumbers.add(number); //Add it to the list
                    }

                    phoneCur.close(); //Close to phone table query
                }
                else return new String[] {"N/A"}; //If no data return N/A
            }
            else return new String[] {"N/A"}; //If contact not found return N/A
            cur.close(); //Close the contacts table query
        }

        //Convert list to array
        String[] returnArray = new String[phoneNumbers.size()];
        phoneNumbers.toArray(returnArray);
        return returnArray; //Return the array of phoneNumber for the contact
    }

    String[] getContactEmailAddresses(Integer contactID) //Get the email addresses of the contact by id
    {
        ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
        String strID = String.valueOf(contactID); //Convert the id to string
        List<String> emailAddress = new ArrayList<>(); //Create a list of email addresses
        Cursor cur = cr.query( //Create a query
                ContactsContract.CommonDataKinds.Email.CONTENT_URI, //Email address table
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", //Where the contact id = ?
                new String[] {strID}, //Replace ? with the contact ID (prevent's sql injection)
                null
        );

        if (cur != null && cur.getCount() > 0) //If can query, data available
        {
            while (cur.moveToNext()) //Move to next email address
            {
                emailAddress.add(cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))); //Add the email address to the list
            }

            cur.close(); //Close the query
        }
        else return new String[] {"N/A"}; //If no data return N/A

        //Convert list to array
        String[] returnArray = new String[emailAddress.size()];
        emailAddress.toArray(returnArray);

        return returnArray; //Return array of emails
    }

    String getContactAddress(Integer contactID) //Get the contact's physical address
    {
        ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
        String strID = String.valueOf(contactID); //Convert id to string
        String address; //String containing the address
        Cursor cur = cr.query( //Create a query
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, //Structured Postal (address) table
                null,
                ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + " = ?", //Where contact ID = ?
                new String[] { strID }, //replace ? with contact id (prevent' sql injection)
                null
        );

        if (cur != null && cur.getCount() > 0) //If can query, data available
        {
            cur.moveToFirst(); //Move to the first match
            address = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)); //get the address
            cur.close(); //Close the query
            return address; //Return the address
        }
        else return "N/A"; //If no data return  N/A
    }

    String getContactNote(Integer contactID) //Get Contacts additional notes by id
    {
        ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
        String strID = String.valueOf(contactID); //Convert id to string
        String note = "N/A"; //string for the note
        Cursor cur = cr.query( //Create a query
                ContactsContract.Data.CONTENT_URI, //Query the contacts data table
                null,
                ContactsContract.CommonDataKinds.Note.CONTACT_ID + " = ?" + //Where contact id = ?
                " AND " + ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE + "'", //And MIME type = Note's mime type
                new String[] {strID}, //Replace ? with the contact id (prevent's sql injection)
                null
        );

        if (cur != null && cur.getCount() > 0) //If can query, data available
        {
            while (cur.moveToNext()) //Move to next match
            {
                note = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)); //Get the note
            }
            cur.close(); //Close the query
            if (note == null) note = "N/A"; //If value is null set it to N/A
            return note; //Return the note
        }
        else return "N/A"; //If no data return N/A
    }

    void addContact(String contactName, String phoneNumber, String emailAddress, String address, String notes) //Add a new contact
    {
        ArrayList<ContentProviderOperation> insertOperations = new ArrayList<>();

        //Add new place for contact

        insertOperations.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        );

        //Add the name of the contact

        if (!contactName.equals(""))
        insertOperations.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contactName)
                .build()
        );

        //Add the number of the contact

        if (!phoneNumber.equals(""))
            insertOperations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            );

        //Add the email address of the contact

        if (!emailAddress.equals(""))
            insertOperations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.DATA, emailAddress)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                    .build()
            );

        //Add the address of the contact

        if (!address.equals(""))
            insertOperations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.DATA, address)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME)
                    .build()
            );

        //Add the note of the contact

        if (!notes.equals(""))
            insertOperations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                    .build()
            );

        try
        {
            ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, insertOperations); //Run the operations on the Contacts Table
        } catch (Exception e)
        {
            Log.e("Contacts", "Add error: " + e.toString());
        }
    }
}