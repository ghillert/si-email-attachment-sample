package org.springintegration.demo.service;
/*
 * Copyright 2002-2012 the original author or authors.
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.support.MessageBuilder;

/**
 * Parses the E-mail Message and converts each containing message and/or attachment into
 * individual Spring Integration messages.
 *
 * @author Gunnar Hillert
 * @version 1.0
 *
 */
public class EmailTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmailTransformer.class);
	
	public List<Message<?>> transformit(javax.mail.Message mailMessage) {

		final List<Message<?>> messages = new ArrayList<Message<?>>();

		handleMessage(mailMessage, messages);

		return messages;
	}

	public void handleMessage(javax.mail.Message mailMessage, List<Message<?>> messages) {  
		
		Object content;
        
		try {
			content = mailMessage.getContent();
		} catch (IOException e) {
			throw new IllegalStateException("Error while retrieving the email contents.", e);
		} catch (MessagingException e) {
			throw new IllegalStateException("Error while retrieving the email contents.", e);
		} 

	    if (content instanceof String) {  
		       
	    	messages.add(MessageBuilder.withPayload((content)).build());
	    	
	    }  else if (content instanceof Multipart) {  
	    	
	        Multipart multipart = (Multipart)content;  
	        handleMultipart(multipart, mailMessage, messages);  
	        
	    } else {
	    	throw new IllegalStateException("This content type is not supported - " + content.getClass().getSimpleName());
	    }

	}
	
	public void handleMultipart(Multipart multipart, javax.mail.Message mailMessage, List<Message<?>> messages) {  
	    final int count;
		try {
			count = multipart.getCount();
		} catch (MessagingException e) {
			throw new IllegalStateException("Error while retrieving the number of enclosed BodyPart objects.", e);
		}  
	    
	    for (int i = 0; i < count; i++) {  
	    	
	        final BodyPart bp;
	        
			try {
				bp = multipart.getBodyPart(i);
			} catch (MessagingException e) {
				throw new IllegalStateException("Error while retrieving body part.", e);
			}  
			
        	final String contentType;
        	final String filename;
        	final String disposition;
        	final String subject;
        	
        	try {
            	contentType = bp.getContentType();
            	filename    = bp.getFileName();
            	disposition = bp.getDisposition();
            	subject     = mailMessage.getSubject();
            	
			} catch (MessagingException e) {
				throw new IllegalStateException("Unable to retrieve body part meta data.", e);
			}
        	
            if (Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
            	LOGGER.info("Handdling attachment '{}', type: '{}'", filename, contentType);
            }
        	
	        final Object content;
	        
			try {
				content = bp.getContent();
			} catch (IOException e) {
				throw new IllegalStateException("Error while retrieving the email contents.", e);
			} catch (MessagingException e) {
				throw new IllegalStateException("Error while retrieving the email contents.", e);
			} 
	        
	        if (content instanceof String) {  
	        	
	        	final Message<String> message = MessageBuilder.withPayload((String) content)
      	              .setHeader(FileHeaders.FILENAME, subject + ".txt")
      	              .build();
	        	
	            messages.add(message);

	        }  else if (content instanceof InputStream) {  

	        	InputStream inputStream = (InputStream) content;
	        	ByteArrayOutputStream bis = new ByteArrayOutputStream();

	        	try {
					IOUtils.copy(inputStream, bis);
				} catch (IOException e) {
					throw new IllegalStateException("Error while copying input stream to the ByteArrayOutputStream.", e);
				}
	        	
	        	Message<byte[]> message = MessageBuilder.withPayload((bis.toByteArray()))
	        	              .setHeader(FileHeaders.FILENAME, filename)
	        	              .build();
	        	
	        	messages.add(message);
	        	
	        } else if (content instanceof javax.mail.Message)  {  
	        	handleMessage((javax.mail.Message) content, messages);  
	        } else if (content instanceof Multipart) {  
	            Multipart mp2 = (Multipart) content;  
	            handleMultipart(mp2, mailMessage, messages);  
	        } else {
	        	throw new IllegalStateException("Content type not handled: " + content.getClass().getSimpleName());
	        }
	    }  
	    
	}  

	
}
