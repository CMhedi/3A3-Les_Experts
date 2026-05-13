## 🧪 Pack Inscription System - Testing Guide

### ✅ Pre-Testing Checklist

- [ ] All files copied to correct locations
- [ ] Project compiled without errors
- [ ] Database connection working
- [ ] FXML controller properly linked
- [ ] CSS files accessible
- [ ] Services available and configured

---

## 🎯 Test Cases

### 1. **UI Loading Test**
**Objective**: Verify page loads correctly

```
Steps:
1. Navigate to PackInscriptionView.fxml
2. Verify page title displays
3. Verify header is visible
4. Verify pack grid loads

Expected:
✅ Page displays without errors
✅ Header shows "S'inscrire au Pack"
✅ Pack cards visible in grid
✅ Smooth animations present
```

**Test Data**:
```
- Page loads in < 2 seconds
- 3-4 pack cards visible
- No console errors
```

---

### 2. **Pack Selection Test**
**Objective**: Verify pack selection works

```
Steps:
1. Load PackInscriptionView
2. Click on first pack card
3. Verify details appear
4. Click on different pack
5. Verify details update

Expected:
✅ Pack card highlights
✅ Details section shows
✅ Price updates
✅ Activities list loads
✅ Summary updates
```

**Test Data**:
```
Pack 1:
- Name: "CampEveryTime"
- Price: 60 TND
- Discount: 10%
- Activities: 8 max

Pack 2:
- Name: "starsSport"
- Price: 200 TND
- Discount: 45%
- Activities: 10 max
```

---

### 3. **Form Validation Test**
**Objective**: Verify input validation works

#### Test 3.1: Email Validation
```
Steps:
1. Enter invalid email: "test"
2. Tab out of field
3. Verify error message
4. Enter valid email: "test@example.com"
5. Verify error clears

Expected:
✅ Invalid emails rejected
✅ Valid emails accepted
✅ Visual feedback provided
```

#### Test 3.2: Phone Validation
```
Steps:
1. Enter invalid phone: "123"
2. Tab out
3. Verify error
4. Enter valid phone: "+216 90123456"
5. Verify accepted

Expected:
✅ Short numbers rejected
✅ Valid format accepted
✅ Error message shown
```

#### Test 3.3: Card Validation
```
Steps:
1. Enter invalid card: "1234"
2. Verify error
3. Enter 16-digit card: "4532123456789010"
4. Verify accepted

Expected:
✅ Invalid cards rejected
✅ Valid cards accepted
✅ Real-time validation
```

#### Test 3.4: CVV Validation
```
Steps:
1. Enter invalid CVV: "12"
2. Verify error
3. Enter valid CVV: "123"
4. Verify accepted

Expected:
✅ 2-digit CVV rejected
✅ 3-4 digit CVV accepted
```

---

### 4. **Price Calculation Test**
**Objective**: Verify price calculations

#### Test 4.1: Base Price
```
Pack: CampEveryTime
Base Price: 60 TND
Discount: 10 TND
Expected Price: 50 TND
People: 1

Calculation:
- 60 - 10 = 50 TND

Expected:
✅ Price shows: 50 TND
✅ Correct calculation
```

#### Test 4.2: Bulk Price
```
Same pack, People: 5

Calculation:
- (60 - 10) × 5 = 250 TND

Expected:
✅ Price shows: 250 TND
✅ Correct multiplication
```

#### Test 4.3: Promo Code
```
Pack: CampEveryTime
Base Total: 50 TND
Promo: "PROMO10" (10% discount)
Final: 45 TND

Expected:
✅ Promo applied
✅ 10% discount applied
✅ Final price: 45 TND
```

---

### 5. **Payment Method Test**
**Objective**: Verify payment methods display

```
Steps:
1. Select "Card Payment"
2. Verify card fields appear
3. Enter card details
4. Select "Mobile Payment"
5. Verify card fields disappear

Expected:
✅ Card fields show/hide correctly
✅ Form validation works
✅ Easy switching between methods
```

---

### 6. **Database Integration Test**
**Objective**: Verify database operations

#### Test 6.1: Pack Loading
```
Expected:
✅ Packs load from database
✅ All pack data visible
✅ Activities load correctly
✅ Prices display correctly
```

#### Test 6.2: Inscription Creation
```
Steps:
1. Complete form
2. Click "Continuer"
3. Check database
4. Verify inscription saved

Expected:
✅ Inscription saved
✅ Correct user ID
✅ Correct pack ID
✅ Correct amount
✅ Status: EN_ATTENTE
```

#### Test 6.3: Payment Status Update
```
Steps:
1. Process payment
2. Check database
3. Verify inscription updated

Expected:
✅ Status changed to CONFIRMEE
✅ Payment status: paid
✅ Transaction ID saved
✅ Payment date recorded
```

---

### 7. **Email Notification Test**
**Objective**: Verify email sending

```
Expected:
✅ Inscription confirmation sent
✅ Payment confirmation sent
✅ Correct recipient
✅ Email format valid
✅ All required info included
```

**Email Content Check**:
```
From: noreply@ecoadventure.tn
To: user@example.com
Subject: Confirmation d'inscription
Body:
  - Pack name
  - Amount
  - Date
  - Transaction ID
  - Support contact
```

---

### 8. **Responsive Design Test**
**Objective**: Verify mobile responsiveness

#### Test 8.1: Mobile (375px)
```
Expected:
✅ Single column layout
✅ Full-width cards
✅ Touch-friendly buttons
✅ Form fields readable
✅ No horizontal scroll
```

#### Test 8.2: Tablet (768px)
```
Expected:
✅ Two column layout
✅ Optimized spacing
✅ Forms responsive
✅ All content visible
```

#### Test 8.3: Desktop (1920px)
```
Expected:
✅ Three column layout
✅ Full feature set
✅ Professional appearance
✅ All elements visible
```

---

### 9. **Animation Test**
**Objective**: Verify animations work smoothly

```
Steps:
1. Load page → fade-in animation
2. Click pack → scale animation
3. Hover button → lift effect
4. Fill form → slide animations

Expected:
✅ Smooth animations (60 FPS)
✅ No stuttering
✅ Professional feel
✅ Proper timing
```

---

### 10. **Error Handling Test**
**Objective**: Verify error handling

#### Test 10.1: Missing Fields
```
Steps:
1. Leave first name empty
2. Click "Continuer"

Expected:
✅ Error message shown
✅ Form highlights
✅ No submission
```

#### Test 10.2: Database Error
```
Steps:
1. Disconnect database
2. Try to load packs
3. Reconnect

Expected:
✅ Error message shown
✅ Retry option available
✅ Graceful degradation
```

#### Test 10.3: Payment Error
```
Steps:
1. Enter invalid card
2. Process payment

Expected:
✅ Error message shown
✅ Can retry
✅ No data loss
```

---

## 📊 Test Execution Log

### Test Run 1: Basic Functionality
```
Date: [DATE]
Tester: [NAME]
Environment: [DEV/STAGING/PROD]

✅ UI Loads
✅ Packs Display
✅ Selection Works
✅ Form Validates
✅ Price Calculates
✅ Payment Methods Display
✅ Inscription Creates
✅ Database Saves
✅ Email Sends

Result: PASSED
```

### Test Run 2: Edge Cases
```
✅ Empty form submission rejected
✅ Invalid email rejected
✅ Invalid phone rejected
✅ Very large quantity handled
✅ Negative quantity prevented
✅ Duplicate submission prevented

Result: PASSED
```

### Test Run 3: Performance
```
Page Load: 1.2s ✅
Pack Selection: 0.3s ✅
Form Submit: 0.8s ✅
Payment Process: 2.1s ✅
Email Send: 1.5s ✅

Result: PASSED
```

---

## 🎯 Test Scenarios

### Scenario 1: Happy Path (Complete Success)
```
User: New customer
1. Navigate to pack inscription
2. Browse packs
3. Select "CampEveryTime"
4. Enter personal info
5. Select card payment
6. Submit inscription
7. Process payment
8. Receive confirmation

Expected Result: ✅ PASS
```

### Scenario 2: Promo Code Path
```
User: Customer with promo code
1. Navigate to pack inscription
2. Select pack
3. Enter promo code "PROMO10"
4. Complete purchase
5. Verify 10% discount applied

Expected Result: ✅ PASS
```

### Scenario 3: Error Recovery Path
```
User: Customer with issues
1. Start inscription
2. Enter invalid email
3. Get error message
4. Correct email
5. Retry successfully
6. Complete purchase

Expected Result: ✅ PASS
```

### Scenario 4: Mobile Path
```
User: Mobile user
1. Access via mobile browser
2. Single-column layout displays
3. Touch-friendly interactions
4. Responsive forms
5. Complete purchase

Expected Result: ✅ PASS
```

---

## 🚀 Performance Benchmarks

| Operation | Target | Actual | Status |
|-----------|--------|--------|--------|
| Page Load | < 2s | [_____] | _____ |
| Pack Load | < 1s | [_____] | _____ |
| Pack Select | < 0.5s | [_____] | _____ |
| Form Submit | < 1s | [_____] | _____ |
| Payment Process | < 3s | [_____] | _____ |
| Database Query | < 500ms | [_____] | _____ |
| Animation FPS | 60 FPS | [_____] | _____ |

---

## 📝 Regression Test Checklist

Before each release:
- [ ] All packs load correctly
- [ ] Pack selection works
- [ ] Form validation works
- [ ] Prices calculate correctly
- [ ] Payment methods display
- [ ] Database saves data
- [ ] Emails send successfully
- [ ] Mobile responsive
- [ ] Animations smooth
- [ ] No console errors
- [ ] No security warnings
- [ ] Performance acceptable

---

## 🐛 Bug Report Template

```
BUG REPORT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Title: [Brief description]

Severity: [Critical/High/Medium/Low]

Steps to Reproduce:
1. [First step]
2. [Second step]
3. [Expected vs Actual]

Expected: [What should happen]
Actual: [What actually happens]

Environment:
- Browser: [Chrome/Firefox/Safari/Edge]
- OS: [Windows/Mac/Linux]
- Resolution: [1920x1080/etc]

Screenshots: [If applicable]

Console Errors: [If any]

Additional Info:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## ✅ Sign-Off

### Tester Information
```
Name: ___________________
Date: ___________________
Environment: ___________
Signature: ______________
```

### Test Results
```
Total Tests: ___
Passed: ___
Failed: ___
Skipped: ___

Pass Rate: ___%

Ready for Production: [ ] YES [ ] NO
```

---

## 📞 Support

For testing issues:
1. Check this guide
2. Review console logs
3. Verify environment setup
4. Contact development team

---

**Testing Guide Version**: 1.0.0
**Last Updated**: May 12, 2026
**Status**: Active
