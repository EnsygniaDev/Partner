namespace OnescanSample {

    using System;
    using System.Configuration;
    using System.Net;
    using System.Security.Cryptography;
    using System.Text;
    using System.Web;

	/// <summary>
	/// Provides HMAC signing support for onescan messages.
	/// </summary>
	public class HMAC {

		/// <summary>
		/// The name of the header that is used to hold the message signature.
		/// </summary>
		public const string HmacHeaderName = "x-onescan-signature";

		/// <summary>
		/// Generating a hash for the input string, using the given key.
		/// </summary>
		/// <param name="input">The input string to hash</param>
		/// <param name="keystring">The key to use when hashing.</param>
		/// <returns>The hex encoded hash.</returns>
		public static string Hash( string input, string keystring ) {
			byte[] hash = null;
			byte[] key = Encoding.UTF8.GetBytes(keystring);
			byte[] byteArray = Encoding.UTF8.GetBytes(input);
	
			using (HMACSHA1 myhmacsha1 = new HMACSHA1(key)) {
				hash = myhmacsha1.ComputeHash(byteArray);
			}
			return string.Join("", Array.ConvertAll(hash, b => b.ToString("x2")));
		}

        public static void ValidateSignature(string input, System.Collections.Specialized.NameValueCollection headers = null) {
            if (headers == null)
            {
                headers = HttpContext.Current.Request.Headers;

            }
            string headerHmac = headers[HMAC.HmacHeaderName];
            if (string.IsNullOrEmpty(headerHmac))
                throw new Exception("HTTP Error: No signature found in the header");
            string calculatedHmac = HMAC.Hash(input, ConfigurationManager.AppSettings["OnescanSecret"]);
			if (calculatedHmac != headerHmac)
                throw new Exception("HTTP Error: Header signature is invalid");
        }
	}
}