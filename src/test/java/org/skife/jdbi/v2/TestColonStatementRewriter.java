/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.tweak.RewrittenStatement;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TestColonStatementRewriter
{
    private ColonPrefixNamedParamStatementRewriter rw;

    @Before
    public void setUp() throws Exception
    {
        this.rw = new ColonPrefixNamedParamStatementRewriter();
    }

    @Test
    public void testNewlinesOkay() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("select * from something\n where id = :id", new Binding(),
                                            new ConcreteStatementContext(new HashMap<String, Object>()));
        assertEquals("select * from something\n where id = ?", rws.getSql());
    }

    @Test
    public void testOddCharacters() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("~* :boo ':nope' _%&^& *@ :id", new Binding(),
                                            new ConcreteStatementContext(new HashMap<String, Object>()));
        assertEquals("~* ? ':nope' _%&^& *@ ?", rws.getSql());
    }

    @Test
    public void testNumbers() throws Exception
    {
        RewrittenStatement rws = rw.rewrite(":bo0 ':nope' _%&^& *@ :id", new Binding(),
                                            new ConcreteStatementContext(new HashMap<String, Object>()));
        assertEquals("? ':nope' _%&^& *@ ?", rws.getSql());
    }

    @Test
    public void testDollarSignOkay() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("select * from v$session", new Binding(),
                                            new ConcreteStatementContext(new HashMap<String, Object>()));
        assertEquals("select * from v$session", rws.getSql());
    }

   @Test
   public void testHashInColumnNameOkay() throws Exception
   {
      RewrittenStatement rws = rw.rewrite(FIND_AGREEMENT_SQL, new Binding(),
                                          new ConcreteStatementContext(new HashMap<String, Object>()));
      assertEquals(FIND_AGREEMENT_SQL.replace( ":agreementId", "?" ), rws.getSql());
   }

    @Test
    public void testBacktickOkay() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("select * from `v$session", new Binding(),
                                            new ConcreteStatementContext(new HashMap<String, Object>()));
        assertEquals("select * from `v$session", rws.getSql());
    }

    @Test
    public void testBailsOutOnInvalidInput() throws Exception
    {
        try {
            rw.rewrite("select * from something\n where id = :\u0087\u008e\u0092\u0097\u009c", new Binding(),
                                            new ConcreteStatementContext(new HashMap<String, Object>()));

            Assert.fail("Expected 'UnableToCreateStatementException' but got none");
        }
        catch (UnableToCreateStatementException e) {
        }
    }

   private static final String FIND_AGREEMENT_SQL =
      "select Agreement.contGuid agreement_id, " +
      "       Agreement.contUpdDte update_date, " +
      "       Agreement.contPMde payment_method_ordinal, " +
      "       Agreement.contTMF type_mode_frequency, " +
      "       Agreement.contAge days_past_due, " +
      "       Agreement.contLPmtDt last_payment_date, " +
      "       Agreement.contNDueDt next_payment_date, " +
      "       Agreement.contLPmt$ last_payment_amount, " +
      "       Agreement.contNPmt$ next_payment_amount, " +
      "       Agreement.contPDue$ past_due_amount, " +
      "       Agreement.contBalDue balance_due, " +
      "       Agreement.contAR auto_renew, " +
      "       Agreement.contARAmt$ renewal_amount, " +
      "       Agreement.contInvPMd is_payment_method_invalid, " +
      "       Agreement.contPmtAmt payment_amount, " +
      "       Agreement.contColor color, " +
      "       Agreement.contSMsg1 status_message_1, " +
      "       Agreement.contSMsg2 status_message_2, " +
      "       Agreement.contSMsg3 status_message_3, " +
      "       Agreement.contColAgy collection_agency, " +
      "       Agreement.contTotVal total_value, " +
      "       Agreement.contRemVal remaining_value, " +
      "       Agreement.contAmtCol amount_collected, " +
      "       Agreement.contFrzSDt freeze_start_date, " +
      "       Agreement.contFrzEDt freeze_end_date, " +
      "       Agreement.contFrzTyp freeze_type, " +
      "       Agreement.contWrODte write_off_date, " +
      "       Agreement.contWrOAmt write_off_amount, " +
      "       Agreement.contAlwCol is_send_to_collections, " +
      "       Agreement.contCnxDte cancel_date, " +
      "       Agreement.contCnxAmt cancel_amount, " +
      "       Agreement.contSts status, " +
      "       Agreement.contClub club_number, " +
      "       Agreement.contMemb member_number, " +
      "       Agreement.contSgnDte sign_date, " +
      "       Agreement.contStrDte start_date, " +
      "       Agreement.contEndDte end_date, " +
      "       Agreement.contTerm term, " +
      "       Agreement.contDwnAmt down_payment_amount, " +
      "       Agreement.ch_contract_guid, " +
      "       Agreement.ch_tanning_contract_guid, " +
      "       PTI.token payment_token_id, " +
      "       PTI.type payment_token_type, " +
      "       PTI.account_last_four payment_token_last_four_digits, " +
      "       PTI.cc_expiration_date payment_token_expiration_date, " +
      "       Agreement.renewal_draft_date, " +
      "       Agreement.renewal_payments, " +
      "       Agreement.renewal_frequency, " +
      "       AgreementExtension.extRnwCpn$ renewal_coupon_amount, " +
      "       Agreement.duedate_effective_date, " +
      "       Agreement.duedate_day_of_month, " +
      "       coalesce(Promo.master_clientguid, Agreement.clubGuid) club_id, " +
      "       Agreement.paymentGuid payment_id, " +
      "       AgreementExtension.ext1stDue first_payment_date, " +
      "       AgreementExtension.extCpnAmt coupon_payment_amount, " +
      "       AgreementExtension.extInvoic# payment_count, " +
      "       MasterAgreementMember.pCntGuid master_agreement_id, " +
      "       AgreementExtension.authorizedby authorized_by " +
      " from Provision.dbo.ContFile Agreement " +
      " join Provision.dbo.CONTFEXT AgreementExtension on AgreementExtension.extGuid = Agreement.contGuid " +
      " left outer join Provision.dbo.paymentTokenInformation PTI on PTI.token = Agreement.paymentGuid " +
      " left outer join Provision.dbo.PROMOXREF Promo on Promo.promo_clientguid = Agreement.clubGuid " +
      " left outer join Provision.dbo.MembFile MasterAgreementMember WITH (READPAST) " +
      "   on convert(nvarchar, MasterAgreementMember.membclub) + right( '00000' + convert(nvarchar, MasterAgreementMember.membmemb), 5) = AgreementExtension.extMasMbr#" +
      "  and MasterAgreementMember.membord = 9 " +
      "  and MasterAgreementMember.MembSts = 0 " +
      "  and MasterAgreementMember.do_not_send_to_clubhub = 0 " +
      " where Agreement.contGuid = :agreementId " +
      "   and Agreement.do_not_send_to_clubHub = '0'";
}
