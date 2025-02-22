/*
 * Copyright 2013-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

 package de.schildbach.wallet.ui;

 import java.io.ByteArrayOutputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.security.KeyStore;
 import java.security.KeyStoreException;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.regex.Pattern;
 
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
 import java.math.BigInteger;
 import java.security.GeneralSecurityException;
 import java.text.Normalizer;
 import java.util.Arrays;
 
 import static com.google.common.base.Preconditions.checkNotNull;
 import static com.google.common.base.Preconditions.checkState;
 
 import javax.annotation.Nullable;
 import javax.crypto.Cipher;
 import javax.crypto.spec.SecretKeySpec;
 
 import org.bitcoin.protocols.payments.Protos;
 import org.bitcoinj.core.Address;
 import org.bitcoinj.core.AddressFormatException;
 import org.bitcoinj.core.Base58;
 import org.bitcoinj.core.DumpedPrivateKey;
 import org.bitcoinj.core.NetworkParameters;
 import org.bitcoinj.core.ProtocolException;
 import org.bitcoinj.core.Transaction;
 import org.bitcoinj.core.VerificationException;
 import org.bitcoinj.core.VersionedChecksummedBytes;
 
 
 // import org.bitcoinj.crypto.BIP38PrivateKey;
 
 import org.bitcoinj.crypto.TrustStoreLoader;
 import org.bitcoinj.protocols.payments.PaymentProtocol;
 import org.bitcoinj.protocols.payments.PaymentProtocol.PkiVerificationData;
 import org.bitcoinj.protocols.payments.PaymentProtocolException;
 import org.bitcoinj.protocols.payments.PaymentSession;
 import org.bitcoinj.uri.BitcoinURI;
 import org.bitcoinj.uri.BitcoinURIParseException;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import android.content.Context;
 import android.content.DialogInterface.OnClickListener;
 
 import com.google.common.hash.Hashing;
 import com.google.protobuf.InvalidProtocolBufferException;
 import com.google.protobuf.UninitializedMessageException;
 
 import de.schildbach.wallet.Constants;
 import de.schildbach.wallet.data.PaymentIntent;
 import de.schildbach.wallet.util.Io;
 import de.schildbach.wallet.util.Qr;
 import de.schildbach.wallet_test.R;
 
 
 
 /**
  * @author Andreas Schildbach
  */
  
 public abstract class InputParser
 {
	 private static final Logger log = LoggerFactory.getLogger(InputParser.class);
 
	 public abstract static class StringInputParser extends InputParser
	 {
		 private final String input;
 
		 public StringInputParser(final String input)
		 {
			 this.input = input;
		 }
 
		 @Override
		 public void parse()
		 {
			 if (input.startsWith("FERRITE:-"))
			 {
				 try
				 {
					 final byte[] serializedPaymentRequest = Qr.decodeBinary(input.substring(9));
 
					 parseAndHandlePaymentRequest(serializedPaymentRequest);
				 }
				 catch (final IOException x)
				 {
					 log.info("i/o error while fetching payment request", x);
 
					 error(R.string.input_parser_io_error, x.getMessage());
				 }
				 catch (final PaymentProtocolException.PkiVerificationException x)
				 {
					 log.info("got unverifyable payment request", x);
 
					 error(R.string.input_parser_unverifyable_paymentrequest, x.getMessage());
				 }
				 catch (final PaymentProtocolException x)
				 {
					 log.info("got invalid payment request", x);
 
					 error(R.string.input_parser_invalid_paymentrequest, x.getMessage());
				 }
			 }
			 else if (input.startsWith("ferrite:"))
			 {
				 try
				 {
					 final BitcoinURI bitcoinUri = new BitcoinURI(null, input);
					 final Address address = bitcoinUri.getAddress();
					 if (address != null && !Constants.NETWORK_PARAMETERS.equals(address.getParameters()))
						 throw new BitcoinURIParseException("mismatched network");
 
					 handlePaymentIntent(PaymentIntent.fromBitcoinUri(bitcoinUri));
				 }
				 catch (final BitcoinURIParseException x)
				 {
					 log.info("got invalid bitcoin uri: '" + input + "'", x);
 
					 error(R.string.input_parser_invalid_bitcoin_uri, input);
				 }
			 }
			 else if (PATTERN_BITCOIN_ADDRESS.matcher(input).matches())
			 {
				 try
				 {
					 final Address address = Address.fromBase58(Constants.NETWORK_PARAMETERS, input);
 
					 handlePaymentIntent(PaymentIntent.fromAddress(address, null));
				 }
				 catch (final AddressFormatException x)
				 {
					 log.info("got invalid address", x);
 
					 error(R.string.input_parser_invalid_address);
				 }
			 }
			 else if (PATTERN_DUMPED_PRIVATE_KEY_UNCOMPRESSED.matcher(input).matches()
					 || PATTERN_DUMPED_PRIVATE_KEY_COMPRESSED.matcher(input).matches())
			 {
				 try
				 {
					 final VersionedChecksummedBytes key = DumpedPrivateKey.fromBase58(Constants.NETWORK_PARAMETERS, input);
 
					 handlePrivateKey(key);
				 }
				 catch (final AddressFormatException x)
				 {
					 log.info("got invalid address", x);
 
					 error(R.string.input_parser_invalid_address);
				 }
			 }
			 else if (PATTERN_BIP38_PRIVATE_KEY.matcher(input).matches())
			 {
				 try
				 {
					 final VersionedChecksummedBytes key = BIP38PrivateKey.fromBase58(Constants.NETWORK_PARAMETERS, input);
 
					 handlePrivateKey(key);
				 }
				 catch (final AddressFormatException x)
				 {
					 log.info("got invalid address", x);
 
					 error(R.string.input_parser_invalid_address);
				 }
			 }
			 else if (PATTERN_TRANSACTION.matcher(input).matches())
			 {
				 try
				 {
					 final Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, Qr.decodeDecompressBinary(input));
 
					 handleDirectTransaction(tx);
				 }
				 catch (final IOException x)
				 {
					 log.info("i/o error while fetching transaction", x);
 
					 error(R.string.input_parser_invalid_transaction, x.getMessage());
				 }
				 catch (final ProtocolException x)
				 {
					 log.info("got invalid transaction", x);
 
					 error(R.string.input_parser_invalid_transaction, x.getMessage());
				 }
			 }
			 else
			 {
				 cannotClassify(input);
			 }
		 }
 
		 protected void handlePrivateKey(final VersionedChecksummedBytes key)
		 {
			 final Address address = new Address(Constants.NETWORK_PARAMETERS, ((DumpedPrivateKey) key).getKey().getPubKeyHash());
 
			 handlePaymentIntent(PaymentIntent.fromAddress(address, null));
		 }
	 }
 
	 public abstract static class BinaryInputParser extends InputParser
	 {
		 private final String inputType;
		 private final byte[] input;
 
		 public BinaryInputParser(final String inputType, final byte[] input)
		 {
			 this.inputType = inputType;
			 this.input = input;
		 }
 
		 @Override
		 public void parse()
		 {
			 if (Constants.MIMETYPE_TRANSACTION.equals(inputType))
			 {
				 try
				 {
					 final Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, input);
 
					 handleDirectTransaction(tx);
				 }
				 catch (final VerificationException x)
				 {
					 log.info("got invalid transaction", x);
 
					 error(R.string.input_parser_invalid_transaction, x.getMessage());
				 }
			 }
			 else if (PaymentProtocol.MIMETYPE_PAYMENTREQUEST.equals(inputType))
			 {
				 try
				 {
					 parseAndHandlePaymentRequest(input);
				 }
				 catch (final PaymentProtocolException.PkiVerificationException x)
				 {
					 log.info("got unverifyable payment request", x);
 
					 error(R.string.input_parser_unverifyable_paymentrequest, x.getMessage());
				 }
				 catch (final PaymentProtocolException x)
				 {
					 log.info("got invalid payment request", x);
 
					 error(R.string.input_parser_invalid_paymentrequest, x.getMessage());
				 }
			 }
			 else
			 {
				 cannotClassify(inputType);
			 }
		 }
 
		 @Override
		 protected final void handleDirectTransaction(final Transaction transaction) throws VerificationException
		 {
			 throw new UnsupportedOperationException();
		 }
	 }
 
	 public abstract static class StreamInputParser extends InputParser
	 {
		 private final String inputType;
		 private final InputStream is;
 
		 public StreamInputParser(final String inputType, final InputStream is)
		 {
			 this.inputType = inputType;
			 this.is = is;
		 }
 
		 @Override
		 public void parse()
		 {
			 if (PaymentProtocol.MIMETYPE_PAYMENTREQUEST.equals(inputType))
			 {
				 ByteArrayOutputStream baos = null;
 
				 try
				 {
					 baos = new ByteArrayOutputStream();
					 Io.copy(is, baos);
					 parseAndHandlePaymentRequest(baos.toByteArray());
				 }
				 catch (final IOException x)
				 {
					 log.info("i/o error while fetching payment request", x);
 
					 error(R.string.input_parser_io_error, x.getMessage());
				 }
				 catch (final PaymentProtocolException.PkiVerificationException x)
				 {
					 log.info("got unverifyable payment request", x);
 
					 error(R.string.input_parser_unverifyable_paymentrequest, x.getMessage());
				 }
				 catch (final PaymentProtocolException x)
				 {
					 log.info("got invalid payment request", x);
 
					 error(R.string.input_parser_invalid_paymentrequest, x.getMessage());
				 }
				 finally
				 {
					 try
					 {
						 if (baos != null)
							 baos.close();
					 }
					 catch (IOException x)
					 {
						 x.printStackTrace();
					 }
 
					 try
					 {
						 is.close();
					 }
					 catch (IOException x)
					 {
						 x.printStackTrace();
					 }
				 }
			 }
			 else
			 {
				 cannotClassify(inputType);
			 }
		 }
 
		 @Override
		 protected final void handleDirectTransaction(final Transaction transaction) throws VerificationException
		 {
			 throw new UnsupportedOperationException();
		 }
	 }
 
	 public abstract void parse();
 
	 protected final void parseAndHandlePaymentRequest(final byte[] serializedPaymentRequest) throws PaymentProtocolException
	 {
		 final PaymentIntent paymentIntent = parsePaymentRequest(serializedPaymentRequest);
 
		 handlePaymentIntent(paymentIntent);
	 }
 
	 public static PaymentIntent parsePaymentRequest(final byte[] serializedPaymentRequest) throws PaymentProtocolException
	 {
		 try
		 {
			 if (serializedPaymentRequest.length > 50000)
				 throw new PaymentProtocolException("payment request too big: " + serializedPaymentRequest.length);
 
			 final Protos.PaymentRequest paymentRequest = Protos.PaymentRequest.parseFrom(serializedPaymentRequest);
 
			 final String pkiName;
			 final String pkiCaName;
			 if (!"none".equals(paymentRequest.getPkiType()))
			 {
				 final KeyStore keystore = new TrustStoreLoader.DefaultTrustStoreLoader().getKeyStore();
				 final PkiVerificationData verificationData = PaymentProtocol.verifyPaymentRequestPki(paymentRequest, keystore);
				 pkiName = verificationData.displayName;
				 pkiCaName = verificationData.rootAuthorityName;
			 }
			 else
			 {
				 pkiName = null;
				 pkiCaName = null;
			 }
 
			 final PaymentSession paymentSession = PaymentProtocol.parsePaymentRequest(paymentRequest);
 
			 if (paymentSession.isExpired())
				 throw new PaymentProtocolException.Expired("payment details expired: current time " + new Date() + " after expiry time "
						 + paymentSession.getExpires());
 
			 if (!paymentSession.getNetworkParameters().equals(Constants.NETWORK_PARAMETERS))
				 throw new PaymentProtocolException.InvalidNetwork("cannot handle payment request network: " + paymentSession.getNetworkParameters());
 
			 final ArrayList<PaymentIntent.Output> outputs = new ArrayList<PaymentIntent.Output>(1);
			 for (final PaymentProtocol.Output output : paymentSession.getOutputs())
				 outputs.add(PaymentIntent.Output.valueOf(output));
 
			 final String memo = paymentSession.getMemo();
 
			 final String paymentUrl = paymentSession.getPaymentUrl();
 
			 final byte[] merchantData = paymentSession.getMerchantData();
 
			 final byte[] paymentRequestHash = Hashing.sha256().hashBytes(serializedPaymentRequest).asBytes();
 
			 final PaymentIntent paymentIntent = new PaymentIntent(PaymentIntent.Standard.BIP70, pkiName, pkiCaName,
					 outputs.toArray(new PaymentIntent.Output[0]), memo, paymentUrl, merchantData, null, paymentRequestHash);
 
			 if (paymentIntent.hasPaymentUrl() && !paymentIntent.isSupportedPaymentUrl())
				 throw new PaymentProtocolException.InvalidPaymentURL("cannot handle payment url: " + paymentIntent.paymentUrl);
 
			 return paymentIntent;
		 }
		 catch (final InvalidProtocolBufferException x)
		 {
			 throw new PaymentProtocolException(x);
		 }
		 catch (final UninitializedMessageException x)
		 {
			 throw new PaymentProtocolException(x);
		 }
		 catch (final FileNotFoundException x)
		 {
			 throw new RuntimeException(x);
		 }
		 catch (final KeyStoreException x)
		 {
			 throw new RuntimeException(x);
		 }
	 }
 
	 protected abstract void handlePaymentIntent(PaymentIntent paymentIntent);
 
	 protected abstract void handleDirectTransaction(Transaction transaction) throws VerificationException;
 
	 protected abstract void error(int messageResId, Object... messageArgs);
 
	 protected void cannotClassify(final String input)
	 {
		 log.info("cannot classify: '{}'", input);
 
		 error(R.string.input_parser_cannot_classify, input);
	 }
 
	 protected void dialog(final Context context, @Nullable final OnClickListener dismissListener, final int titleResId, final int messageResId,
			 final Object... messageArgs)
	 {
		 final DialogBuilder dialog = new DialogBuilder(context);
		 if (titleResId != 0)
			 dialog.setTitle(titleResId);
		 dialog.setMessage(context.getString(messageResId, messageArgs));
		 dialog.singleDismissButton(dismissListener);
		 dialog.show();
	 }
 
	 // Wallet Import Format (WIF)
	 // WIF SECRET_KEY for Ferritecoin (FEC) is HEX A3 or DEC 163 for mainnet
	 //                                         HEX EF or DEC 239 for testnet
	 // Check private key from 00...00 to ff...ff
	 // Prefix - PrivateKey - Compression - Checksum -> Base58 WIF Private Key
	 // WARNING: NEVER ENTER YOUR PRIVATE KEY IN TO A WEBSITE OR USE A PRIVATE KEY GENERATED ONLINE.
	 private static final Pattern PATTERN_BITCOIN_ADDRESS = Pattern.compile("[" + new String(Base58.ALPHABET) + "]{20,40}");
	 private static final Pattern PATTERN_DUMPED_PRIVATE_KEY_UNCOMPRESSED = Pattern.compile((Constants.NETWORK_PARAMETERS.getId().equals(
			 NetworkParameters.ID_MAINNET) ? "6" : "9")
			 + "[" + new String(Base58.ALPHABET) + "]{50}");
	 private static final Pattern PATTERN_DUMPED_PRIVATE_KEY_COMPRESSED = Pattern.compile((Constants.NETWORK_PARAMETERS.getId().equals(
			 NetworkParameters.ID_MAINNET) ? "R" : "c")
			 + "[" + new String(Base58.ALPHABET) + "]{51}");
	 private static final Pattern PATTERN_BIP38_PRIVATE_KEY = Pattern.compile("6P" + "[" + new String(Base58.ALPHABET) + "]{56}");
	 private static final Pattern PATTERN_TRANSACTION = Pattern.compile("[0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$\\*\\+\\-\\.\\/\\:]{100,}");
	 
	 public static class BIP38PrivateKey extends VersionedChecksummedBytes {
		 private final NetworkParameters params;
		 private final boolean ecMultiply;
		 private final boolean compressed;
		 private final boolean hasLotAndSequence;
		 private final byte[] addressHash;
		 private final byte[] content;
 
		 public BIP38PrivateKey(NetworkParameters params, String encoded) throws AddressFormatException {
			 super(encoded);
			 this.params = checkNotNull(params);
			 if (version != 0x01)
				 throw new AddressFormatException("Mismatched version number: " + version);
			 if (bytes.length != 38)
				 throw new AddressFormatException("Wrong number of bytes, excluding version byte: " + bytes.length);
 
			 hasLotAndSequence = (bytes[1] & 0x04) != 0; // bit 2
			 compressed = (bytes[1] & 0x20) != 0; // bit 5
			 if ((bytes[1] & 0x01) != 0) // bit 0
				 throw new AddressFormatException("Bit 0x01 reserved for future use.");
			 if ((bytes[1] & 0x02) != 0) // bit 1
				 throw new AddressFormatException("Bit 0x02 reserved for future use.");
			 if ((bytes[1] & 0x08) != 0) // bit 3
				 throw new AddressFormatException("Bit 0x08 reserved for future use.");
			 if ((bytes[1] & 0x10) != 0) // bit 4
				 throw new AddressFormatException("Bit 0x10 reserved for future use.");
			 final int byte0 = bytes[0] & 0xff;
			 if (byte0 == 0x42) {
				 // Non-EC-multiplied key
				 if ((bytes[1] & 0xc0) != 0xc0) // bits 6+7
					 throw new AddressFormatException("Bits 0x40 and 0x80 must be set for non-EC-multiplied keys.");
				 ecMultiply = false;
				 if (hasLotAndSequence)
					 throw new AddressFormatException("Non-EC-multiplied keys cannot have lot/sequence.");
			 } else if (byte0 == 0x43) {
				 // EC-multiplied key
				 if ((bytes[1] & 0xc0) != 0x00) // bits 6+7
					 throw new AddressFormatException("Bits 0x40 and 0x80 must be cleared for EC-multiplied keys.");
				 ecMultiply = true;
			 } else {
				 throw new AddressFormatException("Second byte must by 0x42 or 0x43.");
			 }
			 addressHash = Arrays.copyOfRange(bytes, 2, 6);
			 content = Arrays.copyOfRange(bytes, 6, 38);
		 }
 
		 public static BIP38PrivateKey fromBase58(NetworkParameters params, String base58) throws AddressFormatException {
			 return new BIP38PrivateKey(params, base58);
		 }
	 }
	 
 }