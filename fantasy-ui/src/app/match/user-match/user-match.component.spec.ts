import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserMatchComponent } from './user-match.component';

describe('UserMatchComponent', () => {
  let component: UserMatchComponent;
  let fixture: ComponentFixture<UserMatchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserMatchComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserMatchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
