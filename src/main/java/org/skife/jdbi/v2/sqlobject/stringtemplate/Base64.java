/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xerces" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.skife.jdbi.v2.sqlobject.stringtemplate;

import java.nio.charset.Charset;


/**
 * This class provides encode/decode for RFC 2045 Base64 as
 * defined by RFC 2045, N. Freed and N. Borenstein.
 * RFC 2045: Multipurpose Internet Mail Extensions (MIME)
 * Part One: Format of Internet Message Bodies. Reference
 * 1996 Available at: http://www.ietf.org/rfc/rfc2045.txt
 * This class is used by XML Schema binary format validation
 *
 * @author Jeffrey Rodriguez
 * @version     $Id$
 */

final class  Base64 {
    static private final int  BASELENGTH         = 255;
    static private final int  LOOKUPLENGTH       = 63;
    static private final int  TWENTYFOURBITGROUP = 24;
    static private final int  EIGHTBIT           = 8;
    static private final int  SIXTEENBIT         = 16;
    static private final int  SIXBIT             = 6;
    static private final int  FOURBYTE           = 4;


    static private final byte PAD               = ( byte ) '=';
    static private byte [] base64Alphabet       = new byte[BASELENGTH];
    static private byte [] lookUpBase64Alphabet = new byte[LOOKUPLENGTH];

    static {

        for (int i = 0; i<BASELENGTH; i++ ) {
            base64Alphabet[i] = -1;
        }
        for ( int i = 'Z'; i >= 'A'; i-- ) {
            base64Alphabet[i] = (byte) (i-'A');
        }
        for ( int i = 'z'; i>= 'a'; i--) {
            base64Alphabet[i] = (byte) ( i-'a' + 26);
        }

        for ( int i = '9'; i >= '0'; i--) {
            base64Alphabet[i] = (byte) (i-'0' + 52);
        }

        base64Alphabet['+']  = 62;
        base64Alphabet['/']  = 63;

        for (int i = 0; i<=25; i++ )
            lookUpBase64Alphabet[i] = (byte) ('A'+i );

        for (int i = 26,  j = 0; i<=51; i++, j++ )
            lookUpBase64Alphabet[i] = (byte) ('a'+ j );

        for (int i = 52,  j = 0; i<=61; i++, j++ )
            lookUpBase64Alphabet[i] = (byte) ('0' + j );

    }

    public static boolean isBase64( String isValidString ){
        return isArrayByteBase64( isValidString.getBytes(Charset.forName("UTF-8")));
    }


    public static boolean isBase64( byte octect ) {
        //shall we ignore white space? JEFF??
        return octect == PAD || base64Alphabet[octect] != -1;
    }


    public static boolean isArrayByteBase64( byte[] arrayOctect ) {
        int length = arrayOctect.length;
        if ( length == 0 )
            return false;
        for ( int i=0; i < length; i++ ) {
            if ( Base64.isBase64( arrayOctect[i] ) == false)
                return false;
        }
        return true;
    }

    /**
     * Encodes hex octects into Base64
     *
     * @param binaryData Array containing binaryData
     * @return Encoded Base64 array
     */
    public byte[] encode( byte[] binaryData ) {
        int      lengthDataBits    = binaryData.length*EIGHTBIT;
        int      fewerThan24bits   = lengthDataBits%TWENTYFOURBITGROUP;
        int      numberTriplets    = lengthDataBits/TWENTYFOURBITGROUP;
        byte     encodedData[]     = null;


        if ( fewerThan24bits != 0 ) //data not divisible by 24 bit
            encodedData = new byte[ (numberTriplets + 1 )*4  ];
        else // 16 or 8 bit
            encodedData = new byte[ numberTriplets*4 ];

        byte k=0, l=0, b1=0,b2=0,b3=0;

        int encodedIndex = 0;
        int dataIndex   = 0;
        int i           = 0;
        for ( i = 0; i<numberTriplets; i++ ) {

            dataIndex = i*3;
            b1 = binaryData[dataIndex];
            b2 = binaryData[dataIndex + 1];
            b3 = binaryData[dataIndex + 2];

            l  = (byte)(b2 & 0x0f);
            k  = (byte)(b1 & 0x03);

            encodedIndex = i*4;
            encodedData[encodedIndex]   = lookUpBase64Alphabet[ b1 >>2 ];
            encodedData[encodedIndex+1] = lookUpBase64Alphabet[(b2 >>4 ) | ( k<<4 )];
            encodedData[encodedIndex+2] = lookUpBase64Alphabet[ (l <<2 ) | ( b3>>6)];
            encodedData[encodedIndex+3] = lookUpBase64Alphabet[ b3 & 0x3f ];
        }

        // form integral number of 6-bit groups
        dataIndex    = i*3;
        encodedIndex = i*4;
        if (fewerThan24bits == EIGHTBIT ) {
            b1 = binaryData[dataIndex];
            k = (byte) ( b1 &0x03 );
            encodedData[encodedIndex]     = lookUpBase64Alphabet[ b1 >>2 ];
            encodedData[encodedIndex + 1] = lookUpBase64Alphabet[ k<<4 ];
            encodedData[encodedIndex + 2] = PAD;
            encodedData[encodedIndex + 3] = PAD;
        } else if ( fewerThan24bits == SIXTEENBIT ) {

            b1 = binaryData[dataIndex];
            b2 = binaryData[dataIndex +1 ];
            l = ( byte ) ( b2 &0x0f );
            k = ( byte ) ( b1 &0x03 );
            encodedData[encodedIndex]     = lookUpBase64Alphabet[ b1 >>2 ];
            encodedData[encodedIndex + 1] = lookUpBase64Alphabet[ (b2 >>4 ) | ( k<<4 )];
            encodedData[encodedIndex + 2] = lookUpBase64Alphabet[ l<<2 ];
            encodedData[encodedIndex + 3] = PAD;
        }
        return encodedData;
    }


    /**
     * Decodes Base64 data into octects
     *
     * @param binaryData Byte array containing Base64 data
     * @return Array containind decoded data.
     */
    public byte[] decode( byte[] base64Data ) {
        int      numberQuadruple    = base64Data.length/FOURBYTE;
        byte     decodedData[]      = null;
        byte     b1=0,b2=0,b3=0, b4=0, marker0=0, marker1=0;

        // Throw away anything not in base64Data
        // Adjust size

        int encodedIndex = 0;
        int dataIndex    = 0;
        decodedData      = new byte[ numberQuadruple*3 + 1 ];

        for (int i = 0; i<numberQuadruple; i++ ) {
            dataIndex = i*4;
            marker0   = base64Data[dataIndex +2];
            marker1   = base64Data[dataIndex +3];

            b1 = base64Alphabet[base64Data[dataIndex]];
            b2 = base64Alphabet[base64Data[dataIndex +1]];

            if ( marker0 != PAD && marker1 != PAD ) {     //No PAD e.g 3cQl
                b3 = base64Alphabet[ marker0 ];
                b4 = base64Alphabet[ marker1 ];

                decodedData[encodedIndex]   = (byte)(  b1 <<2 | b2>>4 ) ;
                decodedData[encodedIndex+1] = (byte)(((b2 & 0xf)<<4 ) |( (b3>>2) & 0xf) );
                decodedData[encodedIndex+2] = (byte)( b3<<6 | b4 );
            } else if ( marker0 == PAD ) {               //Two PAD e.g. 3c[Pad][Pad]
                decodedData[encodedIndex]   = (byte)(  b1 <<2 | b2>>4 ) ;
                decodedData[encodedIndex+1] = (byte)((b2 & 0xf)<<4 );
                decodedData[encodedIndex+2] = (byte) 0;
            } else if ( marker1 == PAD ) {              //One PAD e.g. 3cQ[Pad]
                b3 = base64Alphabet[ marker0 ];

                decodedData[encodedIndex]   = (byte)(  b1 <<2 | b2>>4 );
                decodedData[encodedIndex+1] = (byte)(((b2 & 0xf)<<4 ) |( (b3>>2) & 0xf) );
                decodedData[encodedIndex+2] = (byte)( b3<<6);
            }
            encodedIndex += 3;
        }
        return decodedData;

    }
}