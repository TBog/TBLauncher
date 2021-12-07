package rocks.tbog.tblauncher.dataprovider;

import java.util.regex.Pattern;

import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.normalizer.PhoneNormalizer;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.Searcher;

public class DialProvider extends SimpleProvider<ContactEntry> {

    // See https://github.com/Neamar/KISS/issues/1137
    private final Pattern phonePattern;

    public DialProvider() {
        phonePattern = Pattern.compile("^[*+0-9# ]{3,}$");
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        // Append an item only if query looks like a phone number and device has phone capabilities
        if (phonePattern.matcher(query).find()) {
            searcher.addResult(getResult(query));
        }
    }

    /**
     * @param phoneNumber phone number to use in the result
     * @return a result that may have a fake id.
     */
    private ContactEntry getResult(String phoneNumber) {
        StringNormalizer.Result normalizedPhone = PhoneNormalizer.simplifyPhoneNumber(phoneNumber);
        ContactEntry pojo = ContactEntry.newDialContact(phoneNumber, normalizedPhone);//new PhonePojo(id, historyId, phoneNumber);
        pojo.setName(phoneNumber, false);
        pojo.setRelevance(pojo.normalizedName, null);
        String phoneNumberAfterFirstCharacter = phoneNumber.substring(1);
        if (!phoneNumberAfterFirstCharacter.contains("*") && !phoneNumberAfterFirstCharacter.contains("+")) {
            // No * and no + (except maybe as a first character), likely to be a phone number and not a Calculator expression
            pojo.boostRelevance(20);
        } else {
            // Query may be a phone number or a calculator expression, more likely to be an expression
            // Calculator expressions have a relevance of 19, so use something lower
            pojo.boostRelevance(15);
        }
        return pojo;
    }
}
