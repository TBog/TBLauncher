package rocks.tbog.tblauncher.result;

import rocks.tbog.tblauncher.ui.ListPopup;

public interface IResultList {
    //void temporarilyDisableTranscriptMode();
    //void updateTranscriptMode(int transcriptMode);

    void launchOccurred();

    void registerPopup(ListPopup popup);
    boolean dismissPopup();
}
