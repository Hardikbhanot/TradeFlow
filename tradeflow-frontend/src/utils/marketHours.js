export function isMarketOpen() {
    const now = new Date();
    // Get IST time
    const istString = now.toLocaleString("en-US", { timeZone: "Asia/Kolkata" });
    const istDate = new Date(istString);

    const day = istDate.getDay();
    const hours = istDate.getHours();
    const minutes = istDate.getMinutes();
    const timeNum = hours * 100 + minutes; // e.g. 9:15 => 915, 15:30 => 1530

    // Mon(1) to Fri(5), between 09:15 and 15:30
    if (day >= 1 && day <= 5 && timeNum >= 915 && timeNum <= 1530) {
        return true;
    }
    return false;
}
